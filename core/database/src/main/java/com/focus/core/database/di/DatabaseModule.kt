package com.focus.core.database.di

import android.content.Context
import androidx.room.Room
import com.focus.core.database.FocusDatabase
import com.focus.core.database.FocusSessionDao
import com.focus.core.database.GoalDao
import com.focus.core.database.PlanDao
import com.focus.core.database.SubjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FocusDatabase =
        Room.databaseBuilder(context, FocusDatabase::class.java, "focus.db").build()

    @Provides
    fun provideFocusSessionDao(db: FocusDatabase): FocusSessionDao = db.focusSessionDao()

    @Provides
    fun provideSubjectDao(db: FocusDatabase): SubjectDao = db.subjectDao()

    @Provides
    fun provideGoalDao(db: FocusDatabase): GoalDao = db.goalDao()

    @Provides
    fun providePlanDao(db: FocusDatabase): PlanDao = db.planDao()
}
