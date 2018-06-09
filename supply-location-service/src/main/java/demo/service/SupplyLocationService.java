package demo.service;

import demo.domain.SupplyLocation;

import java.util.List;

public interface SupplyLocationService {
    List<SupplyLocation> saveSupplyLocationsZipContains503(List<SupplyLocation> locations);
}
