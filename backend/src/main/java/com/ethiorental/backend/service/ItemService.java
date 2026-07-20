package com.ethiorental.backend.service;


import com.ethiorental.backend.dto.*;
import com.ethiorental.backend.mapper.*;
import com.ethiorental.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Transactional
    public ItemResponse create(ItemRequest request) {
        var entity = itemMapper.toEntity(request);
        var saved = itemRepository.save(entity);
        return itemMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> findAll() {
        return itemRepository.findAll()
                .stream()
                .map(itemMapper::toResponse)
                .toList();
    }
}