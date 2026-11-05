package com.pingo.feature.settings.impl

import com.pingo.core.data.AccountRepository
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private val accountRepository = FakeAccountRepository()

    private val viewModel = SettingsViewModel(accountRepository)

    @Test
    fun `exclusao confirmada conclui e sinaliza conta apagada`() = runTest {
        viewModel.showDeleteDialog()
        viewModel.deleteAccount()

        assertTrue(viewModel.uiState.value.accountDeleted)
        assertFalse(viewModel.uiState.value.showDeleteDialog)
    }

    @Test
    fun `falha na exclusao mantem o dialogo e permite tentar de novo`() = runTest {
        accountRepository.failure = IllegalStateException("sem rede")
        viewModel.showDeleteDialog()
        viewModel.deleteAccount()

        assertTrue(viewModel.uiState.value.deleteFailed)
        assertTrue(viewModel.uiState.value.showDeleteDialog)
        assertFalse(viewModel.uiState.value.accountDeleted)

        accountRepository.failure = null
        viewModel.deleteAccount()

        assertTrue(viewModel.uiState.value.accountDeleted)
    }

    @Test
    fun `cancelar fecha o dialogo sem excluir`() = runTest {
        viewModel.showDeleteDialog()
        viewModel.dismissDeleteDialog()

        assertFalse(viewModel.uiState.value.showDeleteDialog)
        assertFalse(viewModel.uiState.value.accountDeleted)
    }

    private class FakeAccountRepository : AccountRepository {
        var failure: Throwable? = null

        override suspend fun recordConsent(version: String) = Unit

        override suspend fun deleteAccount() {
            failure?.let { throw it }
        }
    }
}
