package et.gov.endrms.repository;

import et.gov.endrms.Entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {}