package com.digarivas.quizapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        QuestionSetEntity::class,
        QuestionEntity::class,
        FailedQuestionEntity::class,
        QuizHistoryEntity::class,
        ExamDraftEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun questionSetDao(): QuestionSetDao
    abstract fun questionDao(): QuestionDao
    abstract fun failedQuestionDao(): FailedQuestionDao
    abstract fun quizHistoryDao(): QuizHistoryDao
    abstract fun examDraftDao(): ExamDraftDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** v2: los conjuntos pueden pertenecer a un grupo con nombre. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE question_sets ADD COLUMN groupName TEXT")
            }
        }

        /** v3: exámenes en curso para poder continuarlos donde se dejaron. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `exam_drafts` (" +
                        "`setId` INTEGER NOT NULL, " +
                        "`mode` TEXT NOT NULL, " +
                        "`setName` TEXT NOT NULL, " +
                        "`order` TEXT NOT NULL, " +
                        "`timeLimit` INTEGER NOT NULL, " +
                        "`practice` INTEGER NOT NULL, " +
                        "`answersJson` TEXT NOT NULL, " +
                        "`current` INTEGER NOT NULL, " +
                        "`answered` INTEGER NOT NULL, " +
                        "`total` INTEGER NOT NULL, " +
                        "`startedAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`setId`, `mode`), " +
                        "FOREIGN KEY(`setId`) REFERENCES `question_sets`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exam_drafts_setId` " +
                        "ON `exam_drafts` (`setId`)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quizapp.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
