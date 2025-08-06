package com.backend.service.waitinglist;

import com.backend.dto.request.WaitingListDTO;

public interface WaitingListService {

    void createWaitingList(WaitingListDTO waitingListDTO);
}
