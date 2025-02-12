package com.example.intervalrecorder.data
import android.content.Context
import androidx.room.Room
import com.example.intervalrecorder.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    @Singleton
    @Provides
    fun provideScheduleDao(database: AppDatabase): ScheduleDao {
        return database.scheduleDao()
    }

    @Singleton
    @Provides
    fun provideScheduleRepository(scheduleDao: ScheduleDao): ScheduleRepository {
        return ScheduleRepository(scheduleDao)
    }

    @Singleton
    @Provides
    fun provideDataRepository(): DataRepository {
        return DataRepository()
    }
}

