package io.github.hossensyedriadh.cbs.mandate.internal.util;

import io.github.hossensyedriadh.cbs.mandate.api.command.OpenMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.AmountType;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.CapBehaviour;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.CreditorScheme;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.HolidayAdjustment;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateKind;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateStatus;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.PaymentFrequency;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MandateRulesTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsTheCanonicalIbanAndRejectsABrokenChecksum() {
        assertTrue(IbanCheck.isValid("GB82 WEST 1234 5698 7654 32"));
        assertTrue(IbanCheck.isValid("gb82west12345698765432"));
        assertFalse(IbanCheck.isValid("GB82WEST12345698765433"));
        assertFalse(IbanCheck.isValid(null));
        assertFalse(IbanCheck.isValid("NOT-AN-IBAN"));
    }

    @Test
    void transitionsFollowTheMandateLifecycle() {
        assertTrue(MandateTransitions.canAmend(MandateStatus.ACTIVE));
        assertTrue(MandateTransitions.canSuspend(MandateStatus.PENDING));
        assertFalse(MandateTransitions.canSuspend(MandateStatus.SUSPENDED));
        assertTrue(MandateTransitions.canResume(MandateStatus.SUSPENDED));
        assertFalse(MandateTransitions.canResume(MandateStatus.ACTIVE));
        assertTrue(MandateTransitions.canCancel(MandateStatus.SUSPENDED));
        assertFalse(MandateTransitions.canCancel(MandateStatus.COMPLETED));
        assertTrue(MandateTransitions.canCollect(MandateStatus.ACTIVE));
        assertFalse(MandateTransitions.canCollect(MandateStatus.PENDING));
        assertTrue(MandateTransitions.isTerminal(MandateStatus.EXPIRED));
        assertFalse(MandateTransitions.isTerminal(MandateStatus.ACTIVE));
    }

    @Test
    void openCommandRejectsAnEndDateBeforeTheStart() {
        OpenMandateCommand command = this.standingOrder();
        command.setEndDate(command.getStartDate().minusDays(1));
        assertFalse(this.validator.validate(command).isEmpty());
    }

    @Test
    void openCommandRejectsAPercentAboveOneHundred() {
        OpenMandateCommand command = this.standingOrder();
        command.setAmountType(AmountType.PERCENT_OF_BALANCE);
        command.setAmount(new BigDecimal("150"));
        Set<ConstraintViolation<OpenMandateCommand>> violations = this.validator.validate(command);
        assertFalse(violations.isEmpty());
    }

    @Test
    void openCommandRequiresAnIbanForAnExternalCreditor() {
        OpenMandateCommand command = this.standingOrder();
        command.setCreditorScheme(CreditorScheme.EXTERNAL_IBAN);
        command.setCreditorAccountId(null);
        command.setExternalCreditorName("Northwind Supplies");
        command.setExternalCreditorIban("GB00INVALID");
        assertFalse(this.validator.validate(command).isEmpty());
    }

    @Test
    void openCommandAcceptsACompleteInternalStandingOrder() {
        assertEquals(0, this.validator.validate(this.standingOrder()).size());
    }

    @Test
    void sweepCannotTargetAnExternalIban() {
        OpenMandateCommand command = this.standingOrder();
        command.setKind(MandateKind.SWEEP);
        command.setCreditorScheme(CreditorScheme.EXTERNAL_IBAN);
        command.setCreditorAccountId(null);
        command.setExternalCreditorName("Own Account");
        command.setExternalCreditorIban("GB82WEST12345698765432");
        assertFalse(this.validator.validate(command).isEmpty());
    }

    private OpenMandateCommand standingOrder() {
        OpenMandateCommand command = new OpenMandateCommand();
        command.setDebtorAccountId(10L);
        command.setCreditorAccountId(20L);
        command.setCurrencyId(1L);
        command.setKind(MandateKind.STANDING_ORDER);
        command.setCreditorScheme(CreditorScheme.INTERNAL_ACCOUNT);
        command.setFrequency(PaymentFrequency.MONTHLY);
        command.setAmountType(AmountType.FIXED);
        command.setAmount(new BigDecimal("100.00"));
        command.setDayOfMonth(15);
        command.setStartDate(LocalDate.of(2026, 12, 1));
        command.setHolidayAdjustment(HolidayAdjustment.FOLLOWING);
        command.setCapBehaviour(CapBehaviour.CAP);
        command.setMaxRetries(3);
        return command;
    }
}
