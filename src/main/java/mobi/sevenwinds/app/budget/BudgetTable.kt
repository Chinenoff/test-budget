package mobi.sevenwinds.app.budget

import mobi.sevenwinds.app.author.AuthorResponseRecord
import mobi.sevenwinds.app.author.AuthorTable
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object BudgetTable : IntIdTable("budget") {
    val year = integer("year")
    val month = integer("month")
    val amount = integer("amount")
    val type = enumerationByName("type", 100, BudgetType::class)
    val authorId = reference("author_id", AuthorTable)
}

class BudgetEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BudgetEntity>(BudgetTable)

    var year by BudgetTable.year
    var month by BudgetTable.month
    var amount by BudgetTable.amount
    var type by BudgetTable.type
    var authorId by BudgetTable.authorId

    fun toRecord(): BudgetRecord {
        return BudgetRecord(year, month, amount, type, authorId?.value)
    }

    fun toResponse(author: AuthorResponseRecord?): BudgetResponseRecord {
        return BudgetResponseRecord(year, month, amount, type, author)
    }
}