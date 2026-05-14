package dev.songzh.functiontracer.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

enum class EntryOrExit {
    ENTRY,
    EXIT
}


/** FQ name of the @Trace annotation defined in plugin-annotations. */
private const val TRACE_ANNOTATION_FQ_NAME = "dev.songzh.functiontracer.Trace"

/**
 * IR transformer that wraps function bodies with entry/exit trace calls.
 *
 * For every function that should be traced it:
 *  1. Prepends `traceLog(">>> [TRACE] [thread-id = xxx] Entering <name>", logFile)` to the function body.
 *  2. Transforms every `IrReturn` that targets this function into:
 *     ```
 *     return run {
 *         println("<<< [TRACE] Exiting <name>")
 *         <original-return-value>
 *     }
 *     ```
 *
 * A function is selected for tracing when either:
 *  - `traceAll == true`, or
 *  - the function carries the `@Trace` annotation.
 *
 * Inline and external functions are always skipped.
 */
class FunctionTracerTransformer(
    private val pluginContext: IrPluginContext,
    private val traceAll: Boolean,
    private val logFile: String = "",
) : IrElementTransformerVoid() {

    private val irBuiltIns = pluginContext.irBuiltIns

    /**
     * Lazily resolved reference to `dev.songzh.functiontracer.traceLog(message, logFile)`.
     * This runtime helper writes to a file when logFile is non-empty, otherwise prints to stdout.
     */
    @Suppress("DEPRECATION")
    private val traceLogSymbol by lazy {
        pluginContext.referenceFunctions(
            CallableId(FqName("dev.songzh.functiontracer"), Name.identifier("traceLog"))
        ).first()
    }

    /**
     * Lazily resolved reference to `dev.songzh.functiontracer.traceCurrentThreadId()`.
     * This is the runtime helper (defined in plugin-annotations) that returns the
     * current thread / worker ID as a string.
     */
    @Suppress("DEPRECATION")
    private val threadIdSymbol by lazy {
        pluginContext.referenceFunctions(
            CallableId(FqName("dev.songzh.functiontracer"), Name.identifier("traceCurrentThreadId"))
        ).first()
    }

    // -------------------------------------------------------------------------
    // Main entry point: visit each function, process children first, then wrap.
    // -------------------------------------------------------------------------

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        // Process nested functions / lambdas depth-first.
        declaration.transformChildrenVoid(this)

        val body = declaration.body as? IrBlockBody ?: return declaration

        // Skip functions that cannot (or should not) be traced.
        if (declaration.isInline) return declaration
        if (declaration.isExternal) return declaration

        val shouldTrace = traceAll ||
                declaration.hasAnnotation(FqName(TRACE_ANNOTATION_FQ_NAME))
        if (!shouldTrace) return declaration

        val functionName = buildFunctionName(declaration)

        // Step 1 – Wrap every IrReturn that belongs to this function so that
        //          the exit trace is emitted just before the return value is
        //          evaluated / used.
        body.transformChildrenVoid(ReturnWrapper(declaration.symbol, declaration, functionName))

        // Step 2 – Prepend the entry trace as the very first statement.
        body.statements.add(0, buildPrintlnCall(functionName, EntryOrExit.ENTRY))

        // Step 3 – For Unit-returning functions, append exit trace at the end since
        //          they have no explicit return statement (implicit return).
        if (declaration.returnType == irBuiltIns.unitType) {
            body.statements.add(buildPrintlnCall(functionName, EntryOrExit.EXIT))
        }

        return declaration
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildFunctionName(function: IrSimpleFunction): String = buildString {
        val parent = function.parent
        if (parent is IrClass) {
            val pkg = (parent.parent as? IrPackageFragment)?.packageFqName?.asString()
            if (!pkg.isNullOrEmpty()) {
                append(pkg)
                append(".")
            }
            append(parent.name.asString())
            append(".")
        } else {
            // Top-level function: walk up to IrPackageFragment to get the package name.
            val pkg = (function.parent as? IrPackageFragment)?.packageFqName?.asString()
            if (!pkg.isNullOrEmpty()) {
                append(pkg)
                append(".")
            }
        }
        append(function.name.asString())
    }

    private fun buildPrintlnCall(message: String, entryOrExit: EntryOrExit): IrCall {
        // Build the full message as a string template:
        //   "$message [thread=${traceCurrentThreadId()}]"
        // which compiles to an IrStringConcatenation.
        val threadIdCall = IrCallImpl.fromSymbolOwner(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            symbol = threadIdSymbol,
        )
        // Insert [threadId] immediately after "[TRACE] ":
        //   ">>> [TRACE] [thread_id = <id>] Entering …"
        val entryOrExitArrow = if (entryOrExit == EntryOrExit.EXIT) "<<<" else ">>>"
        val entryOrExitString = if (entryOrExit == EntryOrExit.EXIT) "Exiting" else "Entering"
        val traceTag = "[TRACE]"

        val concatenation = IrStringConcatenationImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = irBuiltIns.stringType,
        ).apply {
            arguments.add(irString("$entryOrExitArrow $traceTag [thread-id = "))
            arguments.add(threadIdCall)
            arguments.add(irString("] $entryOrExitString $message"))
        }
        // Call traceLog(message, logFile) — writes to file or stdout depending on logFile.
        return IrCallImpl.fromSymbolOwner(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            symbol = traceLogSymbol,
        ).apply {
            arguments[0] = concatenation
            arguments[1] = irString(logFile)
        }
    }

    private fun irString(value: String): IrConst =
        IrConstImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irBuiltIns.stringType, IrConstKind.String, value)

    // -------------------------------------------------------------------------
    // Inner transformer – wraps IrReturn nodes for a specific function symbol.
    // -------------------------------------------------------------------------

    private inner class ReturnWrapper(
        private val targetSymbol: IrReturnTargetSymbol,
        private val targetFunction: IrSimpleFunction,
        private val functionName: String,
    ) : IrElementTransformerVoid() {

        // Do NOT recurse into nested function / constructor bodies so that their
        // own IrReturn nodes are left untouched.
        override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement =
            declaration

        override fun visitConstructor(declaration: IrConstructor): IrStatement =
            declaration

        override fun visitReturn(expression: IrReturn): IrExpression {
            // Only handle returns that belong to OUR function.
            if (expression.returnTargetSymbol != targetSymbol) {
                return super.visitReturn(expression)
            }

            val originalValue = expression.value

            // For Unit returns: no temp variable needed — print exit then return Unit.
            if (originalValue.type == irBuiltIns.unitType) {
                val block = IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, originalValue.type)
                block.statements.add(buildPrintlnCall(functionName, EntryOrExit.EXIT))
                block.statements.add(originalValue)
                return IrReturnImpl(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    type = expression.type,
                    returnTargetSymbol = expression.returnTargetSymbol,
                    value = block,
                )
            }

            // For non-Unit returns: evaluate the expression first into a temp variable,
            // THEN print the exit trace, THEN return the temp.
            // This guarantees that any function calls inside <expression> are fully
            // resolved before the exit line is emitted, producing a correct call stack:
            //
            //   val _traceResult = <expression>   // inner calls happen here
            //   println("<<< [TRACE] [thread-id = xxx] Exiting …")
            //   return _traceResult
            val tempVar = buildVariable(
                parent = targetFunction,
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.DEFINED,
                name = Name.identifier("_traceResult"),
                type = originalValue.type,
            ).also {
                it.initializer = originalValue
            }

            val getTemp = IrGetValueImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                tempVar.type,
                tempVar.symbol,
            )

            // The outer block has type Nothing (same as IrReturn).
            val block = IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irBuiltIns.nothingType)
            block.statements.add(tempVar)
            block.statements.add(buildPrintlnCall(functionName, EntryOrExit.EXIT))
            block.statements.add(
                IrReturnImpl(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    type = expression.type,
                    returnTargetSymbol = expression.returnTargetSymbol,
                    value = getTemp,
                )
            )
            return block
        }
    }
}
