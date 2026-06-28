package com.swedbank.bankapi.mapper;

import com.swedbank.bankapi.domain.Transaction;
import com.swedbank.bankapi.dto.TransactionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "account.id", target = "accountId")
    TransactionDto toDto(Transaction transaction);

    default Page<TransactionDto> toDtoPage(Page<Transaction> page) {
        return page.map(this::toDto);
    }
}