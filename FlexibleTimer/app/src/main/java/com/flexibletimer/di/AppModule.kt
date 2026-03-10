package com.flexibletimer.di

import android.content.Context
import android.os.Vibrator
import androidx.room.Room
import com.flexibletimer.data.repository.AppDatabase
import com.flexibletimer.data.repository.SavedSequenceDao
import com.flexibletimer.data.repository.SequenceRepository
import com.flexibletimer.data.repository.SequenceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDao(db: AppDatabase): SavedSequenceDao = db.savedSequenceDao()

    @Provides
    @Singleton
    fun provideVibrator(@ApplicationContext ctx: Context): Vibrator =
        ctx.getSystemService(Vibrator::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSequenceRepository(impl: SequenceRepositoryImpl): SequenceRepository
}
