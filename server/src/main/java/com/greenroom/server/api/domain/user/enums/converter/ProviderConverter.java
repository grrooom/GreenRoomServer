package com.greenroom.server.api.domain.user.enums.converter;

import com.greenroom.server.api.domain.user.enums.Provider;
import jakarta.persistence.AttributeConverter;

import java.util.Arrays;
import java.util.NoSuchElementException;
public class ProviderConverter implements AttributeConverter<Provider,String> {
    @Override
    public String convertToDatabaseColumn(Provider attribute) {

        if(attribute==null)
            return null;
        return attribute.getDescription();
    }

    @Override
    public Provider convertToEntityAttribute(String dbData) {
        if(dbData==null)
            return null;

        return Arrays.stream(Provider.values())
                .filter(e->e.getDescription().toLowerCase().equals(dbData.toLowerCase()))
                .findAny()
                .orElseThrow(NoSuchElementException::new);
    }


}