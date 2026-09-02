package com.univ.learningapp.di

import com.univ.learningapp.data.repository.AuthRepositoryImpl
import com.univ.learningapp.data.repository.LearningRepositoryImpl
import com.univ.learningapp.data.repository.TestRepositoryImpl
import com.univ.learningapp.domain.repository.AuthRepository
import com.univ.learningapp.domain.repository.LearningRepository
import com.univ.learningapp.domain.repository.TestRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindLearningRepository(
        impl: LearningRepositoryImpl
    ): LearningRepository

    @Binds
    @Singleton
    abstract fun bindTestRepository(
        impl: TestRepositoryImpl
    ): TestRepository
}
