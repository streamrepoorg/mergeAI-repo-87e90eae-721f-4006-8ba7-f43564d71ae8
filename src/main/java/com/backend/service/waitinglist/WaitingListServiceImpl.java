package com.backend.service.waitinglist;

import com.backend.dto.request.WaitingListDTO;
import com.backend.model.waitingList.WaitingList;
import com.backend.repository.waitinglist.WaitingListRepository;
import com.backend.shared.exception.AlreadyExistException;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class WaitingListServiceImpl implements WaitingListService{

    @Autowired
    WaitingListRepository waitingListRepository;

    private final ModelMapper modelMapper = new ModelMapper();

    public boolean existingWaitingListUser(String email) {
        return waitingListRepository.findByEmail(email).isPresent();
    }

    @Override
    public void createWaitingList(WaitingListDTO waitingListDTO) {
        if (existingWaitingListUser(waitingListDTO.getEmail())){
            throw new AlreadyExistException("You are already on our waiting list, we will keep you in touch");
        }
        WaitingList newWaitingList =  new WaitingList();
        modelMapper.map(waitingListDTO, newWaitingList);
        newWaitingList.setName(waitingListDTO.getName());
        newWaitingList.setEmail(waitingListDTO.getEmail());
        waitingListRepository.save(newWaitingList);
    }
}
