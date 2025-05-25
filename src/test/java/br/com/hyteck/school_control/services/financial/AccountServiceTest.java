package br.com.hyteck.school_control.services.financial;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.repositories.financial.AccountRepository;
import br.com.hyteck.school_control.repositories.financial.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private ResponsibleRepository responsibleRepository;

    @InjectMocks
    private AccountService accountService;

    private Responsible responsible;
    private String responsibleId = "respId1";
    private String accountId = "accId1";

    @BeforeEach
    void setUp() {
        responsible = Responsible.builder().id(responsibleId).name("Test Responsible").build();
    }

    // Tests for findOrCreateAccount
    @Test
    void findOrCreateAccount_ShouldCreateNewGeneralAccount_WhenNotFound() {
        String accountName = "Cash";
        AccountType type = AccountType.ASSET;
        when(accountRepository.findByTypeAndName(type, accountName)).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.setId("newAccId"); // Simulate ID generation
            return savedAccount;
        });

        Account result = accountService.findOrCreateAccount(accountName, type, null);

        assertNotNull(result);
        assertEquals(accountName, result.getName());
        assertEquals(type, result.getType());
        assertNull(result.getResponsible());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void findOrCreateAccount_ShouldReturnExistingGeneralAccount_WhenFound() {
        String accountName = "Cash";
        AccountType type = AccountType.ASSET;
        Account existingAccount = Account.builder().id("existAccId").name(accountName).type(type).balance(new BigDecimal("100")).build();
        when(accountRepository.findByTypeAndName(type, accountName)).thenReturn(Optional.of(existingAccount));

        Account result = accountService.findOrCreateAccount(accountName, type, null);

        assertEquals(existingAccount, result);
        verify(accountRepository, never()).save(any(Account.class));
    }
    
    @Test
    void findOrCreateAccount_ShouldCreateNewResponsibleAccount_WhenNotFound() {
        String accountName = "A/R Test Responsible";
        AccountType type = AccountType.ASSET;
        when(accountRepository.findByTypeAndResponsibleAndName(type, responsible, accountName)).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.setId("newRespAccId");
            return savedAccount;
        });

        Account result = accountService.findOrCreateAccount(accountName, type, responsible);

        assertNotNull(result);
        assertEquals(accountName, result.getName());
        assertEquals(type, result.getType());
        assertEquals(responsible, result.getResponsible());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void findOrCreateAccount_ShouldReturnExistingResponsibleAccount_WhenFound() {
        String accountName = "A/R Test Responsible";
        AccountType type = AccountType.ASSET;
        Account existingAccount = Account.builder().id("existRespAccId").name(accountName).type(type).responsible(responsible).build();
        when(accountRepository.findByTypeAndResponsibleAndName(type, responsible, accountName)).thenReturn(Optional.of(existingAccount));
        
        Account result = accountService.findOrCreateAccount(accountName, type, responsible);
        
        assertEquals(existingAccount, result);
        verify(accountRepository, never()).save(any(Account.class));
    }


    @Test
    void findOrCreateAccount_ShouldThrowException_WhenNameIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.findOrCreateAccount("", AccountType.ASSET, null));
        assertEquals("Account name cannot be blank.", exception.getMessage());
    }

    @Test
    void findOrCreateAccount_ShouldThrowException_WhenTypeIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.findOrCreateAccount("Test", null, null));
        assertEquals("Account type cannot be null.", exception.getMessage());
    }
    
    // Tests for findOrCreateResponsibleARAccount
    @Test
    void findOrCreateResponsibleARAccount_ShouldCreateNewARAccount_WhenNotFound() {
        when(responsibleRepository.findById(responsibleId)).thenReturn(Optional.of(responsible));
        String expectedARName = "Accounts Receivable - " + responsible.getName();
        // findOrCreateAccount will be called internally, mock its behavior for this specific path
        when(accountRepository.findByTypeAndResponsibleAndName(AccountType.ASSET, responsible, expectedARName)).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.setId("newARAccId");
            return savedAccount;
        });

        Account result = accountService.findOrCreateResponsibleARAccount(responsibleId);

        assertNotNull(result);
        assertEquals(expectedARName, result.getName());
        assertEquals(AccountType.ASSET, result.getType());
        assertEquals(responsible, result.getResponsible());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void findOrCreateResponsibleARAccount_ShouldThrowException_WhenResponsibleNotFound() {
        when(responsibleRepository.findById(responsibleId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> accountService.findOrCreateResponsibleARAccount(responsibleId));
        assertTrue(exception.getMessage().contains("Responsible not found with ID: " + responsibleId));
    }

    // Tests for getAccountBalance
    @Test
    void getAccountBalance_ShouldCalculateCorrectlyForAssetAccount() {
        Account assetAccount = Account.builder().id(accountId).name("Cash").type(AccountType.ASSET).build();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(assetAccount));
        when(ledgerEntryRepository.sumDebitAmountByAccountId(accountId)).thenReturn(new BigDecimal("500.00"));
        when(ledgerEntryRepository.sumCreditAmountByAccountId(accountId)).thenReturn(new BigDecimal("200.00"));

        BigDecimal balance = accountService.getAccountBalance(accountId);
        assertEquals(new BigDecimal("300.00"), balance);
    }

    @Test
    void getAccountBalance_ShouldCalculateCorrectlyForRevenueAccount() {
        Account revenueAccount = Account.builder().id(accountId).name("Tuition Revenue").type(AccountType.REVENUE).build();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(revenueAccount));
        when(ledgerEntryRepository.sumDebitAmountByAccountId(accountId)).thenReturn(new BigDecimal("50.00"));  // e.g. refunds
        when(ledgerEntryRepository.sumCreditAmountByAccountId(accountId)).thenReturn(new BigDecimal("1000.00")); // e.g. fees earned

        BigDecimal balance = accountService.getAccountBalance(accountId);
        assertEquals(new BigDecimal("950.00"), balance); // credits - debits
    }
    
    @Test
    void getAccountBalance_ShouldReturnZero_WhenNoLedgerEntries() {
        Account assetAccount = Account.builder().id(accountId).name("New Asset").type(AccountType.ASSET).build();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(assetAccount));
        when(ledgerEntryRepository.sumDebitAmountByAccountId(accountId)).thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumCreditAmountByAccountId(accountId)).thenReturn(BigDecimal.ZERO);

        BigDecimal balance = accountService.getAccountBalance(accountId);
        assertEquals(BigDecimal.ZERO, balance);
    }

    @Test
    void getAccountBalance_ShouldHandleNullSumsFromRepository() {
        Account assetAccount = Account.builder().id(accountId).name("Test Asset").type(AccountType.ASSET).build();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(assetAccount));
        when(ledgerEntryRepository.sumDebitAmountByAccountId(accountId)).thenReturn(null); // Simulate repo returning null
        when(ledgerEntryRepository.sumCreditAmountByAccountId(accountId)).thenReturn(null); // Simulate repo returning null

        BigDecimal balance = accountService.getAccountBalance(accountId);
        assertEquals(BigDecimal.ZERO, balance); // Service should treat null sums as zero
    }


    @Test
    void getAccountBalance_ShouldThrowException_WhenAccountNotFound() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> accountService.getAccountBalance(accountId));
    }

    // Tests for updateAccountBalance
    @Test
    void updateAccountBalance_ShouldUpdateBalanceInRepository_WhenCalculatedBalanceDiffers() {
        Account accountToUpdate = spy(Account.builder().id(accountId).name("Test").type(AccountType.ASSET).balance(new BigDecimal("100.00")).build());
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(accountToUpdate));
        
        // Mock getAccountBalance internal calls (or the repository calls it makes)
        // For simplicity, directly mock what getAccountBalance would calculate
        // This means LedgerRepository calls are implicitly part of this setup if we were to call the real getAccountBalance
        // Let's assume getAccountBalance (which calls ledger repo) calculates a new balance of 150
        AccountService spiedAccountService = spy(accountService);
        doReturn(new BigDecimal("150.00")).when(spiedAccountService).getAccountBalance(accountId);
        
        // Call the method on the spied service
        spiedAccountService.updateAccountBalance(accountId);

        verify(accountToUpdate).setBalance(new BigDecimal("150.00"));
        verify(accountRepository).save(accountToUpdate);
    }

    @Test
    void updateAccountBalance_ShouldNotSave_WhenCalculatedBalanceIsSame() {
        Account accountToUpdate = Account.builder().id(accountId).name("Test").type(AccountType.ASSET).balance(new BigDecimal("100.00")).build();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(accountToUpdate));

        AccountService spiedAccountService = spy(accountService);
        doReturn(new BigDecimal("100.00")).when(spiedAccountService).getAccountBalance(accountId); // Calculated balance is same as persisted

        spiedAccountService.updateAccountBalance(accountId);

        verify(accountRepository, never()).save(any(Account.class));
    }
}
