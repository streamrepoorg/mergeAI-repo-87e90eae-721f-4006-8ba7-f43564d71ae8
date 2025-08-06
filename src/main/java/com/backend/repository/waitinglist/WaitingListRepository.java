package com.backend.repository.waitinglist;

import com.backend.model.waitingList.WaitingList;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WaitingListRepository extends MongoRepository<WaitingList, String> {
    Optional<WaitingList> findByEmail(String email);

}
