package org.jetbrains.kotlin.compiler.plugin.template.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
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

/** FQ name of the @Trace annotation defined in plugin-annotations. */
private const val TRACE_ANNOTATION_FQ_NAME =
    "org.jetbrains.kotlin.compiler.plugin.template.Trace"

/**
 * IR transformer that wraps function bodies with entry/exit trace calls.
 *
 * For every function that should be traced it:
 *  1. Prepends `println(">>> [TRACE] Entering <name>")` to the function body.
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
) : IrElementTransformerVoid() {

    private val irBuiltIns = pluginContext.irBuiltIns

    /**
     * Lazily resolved reference to `kotlin.io.println(Any?)`.
     * We pick the single-argument overload (dispatch/extension-receiver-free function
     * whose unified [parameters] list has exactly one entry).
     * NOTE: `valueParameters` is deprecated in Kotlin 2.3+; use `parameters` instead.
     */
    private val printlnSymbol by lazy {
        // `parameters` (Kotlin 2.3+ unified list) includes dispatch, extension, and value params.
        // Top-level println(message: Any?) has no dispatch/extension receiver, parameters.size == 1.
        // Use finderForBuiltins() – the recommended successor to the deprecated referenceFunctions().
        val finder = pluginContext.finderForBuiltins()
        val candidates = finder.findFunctions(
            CallableId(FqName("kotlin.io"), Name.identifier("println"))
        ).filter { it.owner.parameters.size == 1 }

        candidates.firstOrNull()
            ?: finder.findFunctions(
                CallableId(FqName("kotlin"), Name.identifier("println"))
            ).first { it.owner.parameters.size == 1 }
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
        body.transformChildrenVoid(ReturnWrapper(declaration.symbol, functionName))

        // Step 2 – Prepend the entry trace as the very first statement.
        body.statements.add(0, buildPrintlnCall(">>> [TRACE] Entering $functionName"))

        return declaration
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildFunctionName(function: IrSimpleFunction): String = buildString {
        val parent = function.parent
        if (parent is IrClass) {
            append(parent.name.asString())
            append(".")
        }
        append(function.name.asString())
    }

    private fun buildPrintlnCall(message: String): IrCall =
        IrCallImpl.fromSymbolOwner(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            symbol = printlnSymbol,
        ).apply {
            arguments[0] =
                IrConstImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    irBuiltIns.stringType,
                    IrConstKind.String,
                    message,
                )
        }

    // -------------------------------------------------------------------------
    // Inner transformer – wraps IrReturn nodes for a specific function symbol.
    // -------------------------------------------------------------------------

    private inner class ReturnWrapper(
        private val targetSymbol: IrReturnTargetSymbol,
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

            // Build: IrBlock { println("<<< ..." ); originalValue }
            val block = IrBlockImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                originalValue.type,
            ).also { block ->
                block.statements.add(buildPrintlnCall("<<< [TRACE] Exiting $functionName"))
                block.statements.add(originalValue)
            }

            return IrReturnImpl(
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                type = expression.type,          // Always Nothing for IrReturn
                returnTargetSymbol = expression.returnTargetSymbol,
                value = block,
            )
        }
    }
}
