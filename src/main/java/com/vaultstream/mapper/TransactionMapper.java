package com.vaultstream.mapper;

import com.vaultstream.dto.TransactionDto;
import com.vaultstream.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TransactionMapper {

    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);

    TransactionDto toDto(Transaction transaction);

    Transaction toEntity(TransactionDto dto);
}