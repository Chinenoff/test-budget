package mobi.sevenwinds.app.budget

import io.ktor.features.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorEntity
import mobi.sevenwinds.app.author.AuthorResponseRecord
import mobi.sevenwinds.app.author.AuthorTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            println(body.authorId)

            val author = body.authorId?.let {
                AuthorEntity.findById(body.authorId)
                    ?: throw NotFoundException("Author with ID ${body.authorId} not found")
            }

            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                if (author != null) {
                    this.authorId = author.id
                }
            }

            return@transaction entity.toRecord()
        }
    }

        suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
            transaction {
                var query = (BudgetTable leftJoin AuthorTable)
                    .slice(BudgetTable.columns + AuthorTable.fullName + AuthorTable.createdAt)
                    .select { BudgetTable.year eq param.year }
                    .orderBy(BudgetTable.month, SortOrder.ASC)
                    .orderBy(BudgetTable.amount, SortOrder.DESC)
                    .limit(param.limit, param.offset)

                if (param.authorName != null) {
                    query = query.andWhere { AuthorTable.fullName.lowerCase().like("%${param.authorName.toLowerCase()}%") }
                }

                val total = query.count()
                val data = query.map {
                    val budgetEntity = BudgetEntity.wrapRow(it)
                    val fullName = it[AuthorTable.fullName] as? String
                    var author: AuthorResponseRecord? = null
                    if (fullName != null) {
                        author = AuthorResponseRecord(fullName, it[AuthorTable.createdAt].toString())
                    }
                    budgetEntity.toResponse(author)
                }

                val sumByType = data.groupBy { it.type.name }.mapValues { it.value.sumOf { v -> v.amount } }

                return@transaction BudgetYearStatsResponse(
                    total = total,
                    totalByType = sumByType,
                    items = data
                )
            }
        }
}