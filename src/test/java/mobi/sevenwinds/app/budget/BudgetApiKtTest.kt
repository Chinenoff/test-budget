package mobi.sevenwinds.app.budget

import io.restassured.RestAssured
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BudgetApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction { BudgetTable.deleteAll() }
    }

    @Test
    fun testBudgetPagination() {

        val records = mutableListOf<BudgetRecord>()
        records.add(BudgetRecord(2020, 5, 10, BudgetType.Приход))
        records.add(BudgetRecord(2020, 5, 5, BudgetType.Приход))
        records.add(BudgetRecord(2020, 5, 20, BudgetType.Приход))
        records.add(BudgetRecord(2020, 5, 30, BudgetType.Приход))
        records.add(BudgetRecord(2020, 5, 40, BudgetType.Приход))
        records.add(BudgetRecord(2030, 1, 1, BudgetType.Расход))

        records.forEach { r -> addRecord(r) }

        val year = 2020
        val offset = 1
        val limit = 3

        val expectedSum = records
            .asSequence()
            .filter { it.year == year }
            .sortedWith(compareBy<BudgetRecord> { it.month }.thenByDescending { it.amount })
            .filterIndexed { index, _ -> index > offset-1}
            .take(limit)
            .filter { r -> r.type == BudgetType.Приход }
            .sumBy { it.amount }

        RestAssured.given()
            .queryParam("limit", limit)
            .queryParam("offset", offset)
            .get("/budget/year/$year/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                Assertions.assertEquals(limit, response.total)
                Assertions.assertEquals(limit, response.items.size)
                Assertions.assertEquals(expectedSum, response.totalByType[BudgetType.Приход.name])
            }
    }

    @Test
    fun testStatsSortOrder() {
        addRecord(BudgetRecord(2020, 5, 100, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 1, 5, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 50, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 1, 30, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 400, BudgetType.Приход))

        // expected sort order - month ascending, amount descending

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assertions.assertEquals(30, response.items[0].amount)
                Assertions.assertEquals(5, response.items[1].amount)
                Assertions.assertEquals(400, response.items[2].amount)
                Assertions.assertEquals(100, response.items[3].amount)
                Assertions.assertEquals(50, response.items[4].amount)
            }
    }

    @Test
    fun testInvalidMonthValues() {
        RestAssured.given()
            .jsonBody(BudgetRecord(2020, -5, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)

        RestAssured.given()
            .jsonBody(BudgetRecord(2020, 15, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)
    }

    private fun addRecord(record: BudgetRecord) {
        RestAssured.given()
            .jsonBody(record)
            .post("/budget/add")
            .toResponse<BudgetRecord>().let { response ->
                Assertions.assertEquals(record, response)
            }
    }
}