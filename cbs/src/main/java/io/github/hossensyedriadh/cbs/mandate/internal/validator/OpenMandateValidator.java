package io.github.hossensyedriadh.cbs.mandate.internal.validator;

import io.github.hossensyedriadh.cbs.mandate.api.command.OpenMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.AmountType;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.CreditorScheme;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateKind;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.PaymentFrequency;
import io.github.hossensyedriadh.cbs.mandate.api.validation.ValidOpenMandate;
import io.github.hossensyedriadh.cbs.mandate.internal.util.IbanCheck;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class OpenMandateValidator implements ConstraintValidator<ValidOpenMandate, OpenMandateCommand> {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Override
    public boolean isValid(OpenMandateCommand command, ConstraintValidatorContext context) {
        if (command == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        boolean valid = true;

        if (command.getStartDate() != null && command.getEndDate() != null && command.getEndDate().isBefore(command.getStartDate())) {
            valid = violation(context, "End date cannot be before the start date", "endDate");
        }

        if (command.getMinAmount() != null && command.getMaxAmount() != null
                && command.getMinAmount().compareTo(command.getMaxAmount()) > 0) {
            valid = violation(context, "Minimum amount cannot exceed the maximum amount", "minAmount");
        }

        if (command.getMonthlyCap() != null && command.getMaxAmount() != null
                && command.getMonthlyCap().compareTo(command.getMaxAmount()) < 0) {
            valid = violation(context, "Monthly cap cannot be below the per-collection maximum", "monthlyCap");
        }

        if (command.getAmountType() == AmountType.PERCENT_OF_BALANCE && command.getAmount() != null
                && command.getAmount().compareTo(ONE_HUNDRED) > 0) {
            valid = violation(context, "Percent of balance cannot exceed 100", "amount");
        }

        if (needsDayOfMonth(command.getFrequency()) && command.getDayOfMonth() == null) {
            valid = violation(context, "Day of month is required for this frequency", "dayOfMonth");
        }

        if (needsDayOfWeek(command.getFrequency()) && command.getDayOfWeek() == null) {
            valid = violation(context, "Day of week is required for this frequency", "dayOfWeek");
        }

        if (command.getCreditorScheme() == CreditorScheme.INTERNAL_ACCOUNT) {
            if (command.getCreditorAccountId() == null) {
                valid = violation(context, "Creditor account is required", "creditorAccountId");
            } else if (command.getCreditorAccountId().equals(command.getDebtorAccountId())) {
                valid = violation(context, "Debtor and creditor accounts must be different", "creditorAccountId");
            }
        }

        if (command.getCreditorScheme() == CreditorScheme.EXTERNAL_IBAN) {
            if (command.getExternalCreditorName() == null || command.getExternalCreditorName().isBlank()) {
                valid = violation(context, "External creditor name is required", "externalCreditorName");
            }
            if (!IbanCheck.isValid(command.getExternalCreditorIban())) {
                valid = violation(context, "A valid IBAN is required", "externalCreditorIban");
            }
        }

        if (command.getKind() == MandateKind.SWEEP && command.getCreditorScheme() != CreditorScheme.INTERNAL_ACCOUNT) {
            valid = violation(context, "A sweep can only pay another account in this bank", "creditorScheme");
        }

        return valid;
    }

    private boolean needsDayOfMonth(PaymentFrequency frequency) {
        return frequency == PaymentFrequency.MONTHLY
                || frequency == PaymentFrequency.QUARTERLY
                || frequency == PaymentFrequency.SEMI_ANNUAL
                || frequency == PaymentFrequency.ANNUAL;
    }

    private boolean needsDayOfWeek(PaymentFrequency frequency) {
        return frequency == PaymentFrequency.WEEKLY || frequency == PaymentFrequency.FORTNIGHTLY;
    }

    private boolean violation(ConstraintValidatorContext context, String message, String property) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(property)
                .addConstraintViolation();
        return false;
    }
}
