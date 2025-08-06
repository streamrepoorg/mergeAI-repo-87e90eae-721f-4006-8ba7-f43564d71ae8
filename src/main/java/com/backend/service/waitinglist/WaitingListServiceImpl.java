package com.backend.service.waitinglist;

import com.backend.dto.request.WaitingListDTO;
import com.backend.model.waitingList.WaitingList;
import com.backend.repository.waitinglist.WaitingListRepository;
import com.backend.shared.exception.AlreadyExistException;
import com.backend.shared.exception.InvalidInputException;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WaitingListServiceImpl implements WaitingListService {

    @Autowired
    private WaitingListRepository waitingListRepository;

    private final ModelMapper modelMapper = new ModelMapper();

    public boolean existingWaitingListUser(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidInputException("Email cannot be null or empty");
        }
        return waitingListRepository.findByEmail(email.trim()).isPresent();
    }

    @Override
    public void createWaitingList(WaitingListDTO waitingListDTO) {
        if (waitingListDTO == null) {
            throw new InvalidInputException("Waiting list request cannot be null");
        }

        String email = waitingListDTO.getEmail();
        String name = waitingListDTO.getName();

        if (email == null || email.trim().isEmpty()) {
            throw new InvalidInputException("Email is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Name is required");
        }
        if (existingWaitingListUser(email)) {
            throw new AlreadyExistException("You are already on our waiting list, we will keep you in touch");
        }

        WaitingList newWaitingList = new WaitingList();
        modelMapper.map(waitingListDTO, newWaitingList);
        newWaitingList.setName(name.trim());
        newWaitingList.setEmail(email.trim());

        try {
            waitingListRepository.save(newWaitingList);
            log.info("Successfully added {} to waiting list", email);
        } catch (Exception e) {
            log.error("Failed to save waiting list entry for {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to process waiting list registration due to a system error", e);
        }
    }
}