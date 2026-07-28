package br.com.luizbrand.worklog.client.integration;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CachedOfficeRepository extends CrudRepository<CachedOffice, String> {
}
