package com.vb.gymmanager.service;

import com.vb.gymmanager.model.Gym;
import com.vb.gymmanager.repository.GymRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GymService {

    private final GymRepo gymRepo;

    @Autowired
    public GymService(GymRepo gymRepo) {
        this.gymRepo = gymRepo;
    }
    public Gym getDefaultGym() {

        Iterable<Gym> gymList = gymRepo.findAll();
        for (Gym gym : gymList) {
            return gym;
        }
        Gym newGym = new Gym();
        newGym.setName("Основной зал");
        gymRepo.save(newGym);
        return newGym;

    }

}
