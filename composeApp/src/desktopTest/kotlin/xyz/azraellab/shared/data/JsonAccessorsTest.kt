package xyz.azraellab.shared.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import xyz.azraellab.shared.o as uiO
import xyz.azraellab.shared.s as uiS

/**
 * Ответ сервера - это словарь, и ничто в контракте не обещает, что поле
 * `user` придёт объектом, а не строкой. Раньше `jsonPrimitive`/`jsonObject` на
 * таком значении БРОСАЛИ `IllegalArgumentException`, и разбор падал мимо
 * `AppException`: экран падал целиком вместо показа «поля нет».
 *
 * Здесь два почти одинаковых набора хелперов (data/Json.kt и ui/ScreenKit.kt) -
 * проверяем оба, иначе исправление можно откатить в одной копии незаметно.
 */
class JsonAccessorsTest {

    private val nested = buildJsonObject { put("id", 1) }
    private val list = buildJsonArray { }

    private fun mixed() = buildJsonObject {
        put("str", "текст")
        put("int", 7)
        put("boolTrue", true)
        put("boolFalse", false)
        put("nul", JsonNull)
        put("obj", nested)
        put("arr", list)
    }

    @Test
    fun primitivesReadBackCorrectly() {
        val o = mixed()
        assertEquals("текст", o.s("str"))
        assertEquals(7, o.i("int"))
        assertTrue(o.b("boolTrue"))
        assertFalse(o.b("boolFalse"))
        assertEquals(nested, o.o("obj"))
        assertEquals(list.toList(), o.a("arr"))
    }

    @Test
    fun nonPrimitiveYieldsNothingInsteadOfThrowing() {
        val o = mixed()
        // Раньше каждая из этих строк бросала IllegalArgumentException.
        assertNull(o.s("obj"))
        assertNull(o.s("arr"))
        assertNull(o.s("nul"))
        assertNull(o.i("arr"))
        assertNull(o.l("obj"))
        assertFalse(o.b("obj"), "объект не должен читаться как boolean")
        assertFalse(o.b("arr"))
        assertFalse(o.b("nul"), "JsonNull - это не true")
        assertNull(o.o("arr"), "массив не является объектом")
        assertNull(o.o("nul"))
        assertNull(o.o("str"))
    }

    @Test
    fun missingKeysYieldNothing() {
        val o = mixed()
        assertNull(o.s("нетТакого"))
        assertNull(o.o("нетТакого"))
        assertNull(o.i("нетТакого"))
        assertEquals(emptyList(), o.a("нетТакого"))
    }

    @Test
    fun numericStringsDoNotSilentlyCoerceIntoNumbers() {
        // Нумерация id приходит строкой, а счётчики - числом. Наоборот путать
        // нельзя: нечисловая строка обязана остаться null, а не «0».
        val o = buildJsonObject { put("v", "не число") }
        assertNull(o.i("v"))
        assertNull(o.l("v"))
        assertEquals("не число", o.s("v"))
    }

    @Test
    fun arrayAccessorIgnoresNonArrayValues() {
        val o = mixed()
        assertEquals(emptyList(), o.a("obj"), "объект на месте массива - пустой список, не исключение")
        assertEquals(emptyList(), o.a("nul"))
    }

    // ---- та же гарантия для дублирующего набора в ui/ScreenKit.kt ----------

    @Test
    fun screenKitAccessorsAgreeWithDataAccessors() {
        val o = mixed()
        assertEquals(o.s("str"), o.uiS("str"))
        assertEquals(o.s("obj"), o.uiS("obj"), "ScreenKit.s обязан вести себя как Json.s")
        assertEquals(o.s("arr"), o.uiS("arr"))
        assertEquals(o.s("nul"), o.uiS("nul"))
        assertEquals(o.o("obj"), o.uiO("obj"))
        assertNull(o.uiO("arr"), "ScreenKit.o обязан вести себя как Json.o")
        assertNull(o.uiO("str"))
    }

    // ---- jsonObjOrNull ----------------------------------------------------

    @Test
    fun jsonObjOrNullOnlyPassesRealObjects() {
        assertEquals(nested, nested.jsonObjOrNull())
        assertNull(JsonArray(list).jsonObjOrNull(), "массив - не объект")
        assertNull(JsonNull.jsonObjOrNull())
        assertNull(JsonPrimitive("строка").jsonObjOrNull())
    }
}
