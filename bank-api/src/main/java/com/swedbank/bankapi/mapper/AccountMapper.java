package com.swedbank.bankapi.mapper;

import com.swedbank.bankapi.domain.Account;
import com.swedbank.bankapi.dto.AccountDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(source = "user.id", target = "userId")
    AccountDto toDto(Account account);
}