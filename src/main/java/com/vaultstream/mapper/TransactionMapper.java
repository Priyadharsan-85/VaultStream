package com.vaultstream.mapper;

import com.vaultstream.dto.TransactionDto;
import com.vaultstream.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);

    TransactionDto toDto(Transaction transaction);

    @Mapping(target = "createdAt", ignore = true)
    Transaction toEntity(TransactionDto dto);
}
