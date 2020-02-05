package main.service;

import main.model.Gym;
import main.repository.GymRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GymService {

    @Autowired
    private GymRepo gymRepo;

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
