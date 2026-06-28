package com.example.location_where.di

import android.content.Context
import androidx.room.Room
import com.example.location_where.data.local.LocationDao
import com.example.location_where.data.local.LocationDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LocationDatabase {
        return Room.databaseBuilder(
            context,
            LocationDatabase::class.java,
            "location_database"
        ).build()
    }

    @Provides
    fun provideLocationDao(database: LocationDatabase): LocationDao {
        return database.locationDao()
    }
}
