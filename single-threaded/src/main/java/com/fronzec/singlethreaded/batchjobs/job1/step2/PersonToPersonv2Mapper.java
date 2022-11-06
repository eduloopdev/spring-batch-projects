package com.fronzec.singlethreaded.batchjobs.job1.step2;

import com.fronzec.singlethreaded.person.PersonsEntity;
import com.fronzec.singlethreaded.personv2.PersonsV2Entity;
import java.math.BigDecimal;
import java.util.UUID;

public class PersonToPersonv2Mapper {

  private PersonToPersonv2Mapper() {

  }

  public static PersonsV2Entity fromTo(PersonsEntity entity) {
    PersonsV2Entity personsV2Entity = new PersonsV2Entity();
    personsV2Entity.setEmail(entity.getEmail());
    personsV2Entity.setFirstName(entity.getFirstName());
    personsV2Entity.setLastName(entity.getLastName());
    personsV2Entity.setProfession(entity.getProfession());
    personsV2Entity.setUuidV4(UUID.randomUUID()
            .toString());
    personsV2Entity.setSalary(BigDecimal.valueOf(Math.random()));

    return personsV2Entity;
  }
}