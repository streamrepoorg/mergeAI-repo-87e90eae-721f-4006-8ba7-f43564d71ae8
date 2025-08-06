package com.backend.controller.waitinglist;

import com.backend.dto.request.WaitingListDTO;
import com.backend.dto.response.ResponseDetails;
import com.backend.service.waitinglist.WaitingListServiceImpl;
import com.backend.shared.exception.AlreadyExistException;
import com.backend.shared.exception.InvalidInputException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/waiting-list")
@Slf4j
public class WaitingListController {

    @Autowired
    private WaitingListServiceImpl waitingListService;

    @PostMapping("/user")
    public ResponseEntity<?> createWaitingList(@Valid @RequestBody WaitingListDTO newWaitingList) {
        try {
            waitingListService.createWaitingList(newWaitingList);
            ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), "Successfully added to waiting list", HttpStatus.CREATED.toString(), "/waiting-list/user");
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDetails);
        } catch (AlreadyExistException e) {
            log.warn("Duplicate waiting list entry attempt for email: {}", newWaitingList.getEmail());
            ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), e.getMessage(), HttpStatus.CONFLICT.toString(),"/waiting-list/user");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(responseDetails);
        } catch (InvalidInputException e) {
            log.error("Invalid input for waiting list: {}", e.getMessage());
            ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), e.getMessage(), HttpStatus.BAD_REQUEST.toString(), "/waiting-list/user");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDetails);
        } catch (Exception e) {
            log.error("Unexpected error during waiting list registration: {}", e.getMessage(), e);
            ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), "An unexpected error occurred, please try again later", HttpStatus.INTERNAL_SERVER_ERROR.toString(), "/waiting-list/user");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDetails);
        }
    }
}