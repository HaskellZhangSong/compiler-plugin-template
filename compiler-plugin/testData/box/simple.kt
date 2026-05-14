// WITH_STDLIB
import dev.songzh.functiontracer.Trace
@Trace
fun greet(name: String): String {
    return "Hello, $name!"
}
fun box(): String {
    val result = greet("world")
    return if (result == "Hello, world) "OK" else "Fail: $result"
}
