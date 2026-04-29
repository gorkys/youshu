package com.youshu.app.data.repository

import com.youshu.app.data.local.dao.LocationDao
import com.youshu.app.data.local.entity.Location
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao
) {
    fun getAllLocations(): Flow<List<Location>> = locationDao.getAllLocations()

    fun getRootLocations(): Flow<List<Location>> = locationDao.getRootLocations()

    fun getSubLocations(parentId: Long): Flow<List<Location>> = locationDao.getSubLocations(parentId)

    suspend fun getLocationById(id: Long): Location? = locationDao.getLocationById(id)

    suspend fun insert(location: Location): Long = locationDao.insert(location)

    suspend fun update(location: Location) = locationDao.update(location)

    suspend fun delete(location: Location) = locationDao.delete(location)
}
