package com.vaultstream.mapper;

import com.vaultstream.dto.AccountDto;
import com.vaultstream.model.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountMapper INSTANCE = Mappers.getMapper(AccountMapper.class);

    AccountDto toDto(Account account);

    @Mapping(target = "ownerUser", ignore = true)
    Account toEntity(AccountDto dto);
}
