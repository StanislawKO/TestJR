package com.space.service;

import com.space.controller.ShipOrder;
import com.space.exceptions.BadRequestException;
import com.space.exceptions.NotFoundException;
import com.space.model.Ship;
import com.space.model.ShipType;
import com.space.repository.CosmoportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShipServiceImpl implements ShipService{

    @Autowired
    private CosmoportRepository repo;
    private Calendar prodCal = Calendar.getInstance();

    @Override
    public List<Ship> getShips(String name, String planet, ShipType shipType, Long after, Long before, Boolean isUsed, Double minSpeed, Double maxSpeed, Integer minCrewSize, Integer maxCrewSize, Double minRating, Double maxRating) {
        Iterable<Ship> ships = repo.findAll();
        List<Ship> foundShipsByRequestParams = new ArrayList<>();
        for (Ship ship : ships) {
            prodCal.setTime(ship.getProdDate());
            boolean isAdded = true;
            if (name != null && !ship.getName().toLowerCase().contains(name.toLowerCase()))
                isAdded = false;
            if (planet != null && !ship.getPlanet().toLowerCase().contains(planet.toLowerCase()))
                isAdded = false;
            if (shipType != null) {
                String shipTypeString = shipType.toString();
                if (!ship.getShipType().equals(shipTypeString))
                    isAdded = false;
            }
            if (after != null || before != null) {
                if (after == null)
                    after = 0L;
                if (before == null)
                    before = Long.MAX_VALUE;
                Calendar afterCal = Calendar.getInstance();
                afterCal.setTimeInMillis(after);
                Calendar beforeCal = Calendar.getInstance();
                beforeCal.setTimeInMillis(before);
                if (prodCal.getTimeInMillis() <= afterCal.getTimeInMillis() || prodCal.getTimeInMillis() >= beforeCal.getTimeInMillis())
                    isAdded = false;
            }
            if (isUsed != null && isUsed != ship.getUsed())
                isAdded = false;
            if (minSpeed != null || maxSpeed != null) {
                if (minSpeed == null)
                    minSpeed = Double.MIN_VALUE;
                if (maxSpeed == null)
                    maxSpeed = Double.MAX_VALUE;
                if (ship.getSpeed() <= minSpeed || ship.getSpeed() >= maxSpeed)
                    isAdded = false;
            }
            if (minCrewSize != null || maxCrewSize != null) {
                if (minCrewSize == null)
                    minCrewSize = Integer.MIN_VALUE;
                if (maxCrewSize == null)
                    maxCrewSize = Integer.MAX_VALUE;
                if (ship.getCrewSize() <= minCrewSize || ship.getCrewSize() >= maxCrewSize)
                    isAdded = false;
            }
            if (minRating != null || maxRating != null) {
                if (minRating == null)
                    minRating = Double.MIN_VALUE;
                if (maxRating == null)
                    maxRating = Double.MAX_VALUE;
                if (ship.getRating() <= minRating || ship.getRating() >= maxRating)
                    isAdded = false;
            }
            if  (isAdded)
                foundShipsByRequestParams.add(ship);
        }
        return foundShipsByRequestParams;
    }

    @Override
    public List<Ship> pagingShips(List<Ship> ships, Integer pageNumber, Integer pageSize, ShipOrder shipOrder) {
        if (pageNumber == null)
            pageNumber = 0;
        if (pageSize == null)
            pageSize = 3;
        if (shipOrder == null)
            shipOrder = ShipOrder.ID;
        if (shipOrder.getFieldName().equals(ShipOrder.DATE.getFieldName()))
            return ships.stream()
                    .sorted(Comparator.comparing(Ship::getProdDate))
                    .skip(pageNumber*pageSize)
                    .limit(pageSize).collect(Collectors.toList());
        if (shipOrder.getFieldName().equals(ShipOrder.RATING.getFieldName()))
            return ships.stream()
                    .sorted(Comparator.comparing(Ship::getRating))
                    .skip(pageNumber*pageSize)
                    .limit(pageSize).collect(Collectors.toList());
        if (shipOrder.getFieldName().equals(ShipOrder.SPEED.getFieldName()))
            return ships.stream()
                    .sorted(Comparator.comparing(Ship::getSpeed))
                    .skip(pageNumber*pageSize)
                    .limit(pageSize).collect(Collectors.toList());
        return ships.stream()
                .sorted(Comparator.comparing(Ship::getId))
                .skip(pageNumber*pageSize)
                .limit(pageSize).collect(Collectors.toList());
    }

    @Override
    public Ship getShipById(String id) {

        return repo.findById(idToLong(id)).orElseThrow(NotFoundException::new);
    }

    @Override
    public Ship createShip(Ship ship) {

        checkFielfsForNull(ship);
        checkFieldsForValidValues(ship);
        if (ship.getUsed() == null)
            ship.setUsed(false);
        setRating(ship);

        return repo.saveAndFlush(ship);
    }

    @Override
    public Ship updateShip( String id, Ship ship) {
        Long longId = idToLong(id);
        if (!repo.existsById(longId))
            throw new NotFoundException();

        Ship modifiedShip = repo.findById(longId).get();

        if (ship.getName() != null) {
            if (ship.getName().length() == 0 || ship.getName().length() > 50)
                throw new BadRequestException();
            modifiedShip.setName(ship.getName());
        }

        if (ship.getPlanet() != null) {
            if (ship.getPlanet().length() == 0 || ship.getPlanet().length() > 50)
                throw new BadRequestException();
            modifiedShip.setPlanet(ship.getPlanet());
        }

        if (ship.getShipType() != null)
            modifiedShip.setShipType(ship.getShipType());

        if (ship.getProdDate() != null) {
            prodCal.setTime(ship.getProdDate());
            if (prodCal.get(Calendar.YEAR) < 2800 || prodCal.get(Calendar.YEAR) > 3019)
                throw new BadRequestException();
            modifiedShip.setProdDate(ship.getProdDate());
        }

        if (ship.getSpeed() != null) {
            if (ship.getSpeed() < 0.01 || ship.getSpeed() > 0.99)
                throw new BadRequestException();
            modifiedShip.setSpeed(ship.getSpeed());
        }

        if (ship.getUsed() != null)
            modifiedShip.setUsed(ship.getUsed());

        if (ship.getCrewSize() != null) {
            if (ship.getCrewSize() < 1 || ship.getCrewSize() > 9999)
                throw new BadRequestException();
            modifiedShip.setCrewSize(ship.getCrewSize());
        }

        setRating(modifiedShip);

        return repo.save(modifiedShip);
    }

    @Override
    public void deleteShip(String id) {
        Long longId = idToLong(id);
        if (!repo.existsById(longId))
            throw new NotFoundException();
        repo.deleteById(longId);
    }

    private void checkFielfsForNull(Ship ship) {
        if (ship.getProdDate() == null)
            throw new BadRequestException();
        prodCal.setTime(ship.getProdDate());
        if (ship.getName() == null ||
                ship.getPlanet() == null ||
                ship.getShipType() == null ||
                ship.getProdDate() == null ||
                ship.getSpeed() == null ||
                ship.getCrewSize() == null

        )
            throw new BadRequestException();
    }

    private void checkFieldsForValidValues(Ship ship) {
        prodCal.setTime(ship.getProdDate());
        if (ship.getName().length() == 0 ||
                ship.getName().length() > 50 ||
                ship.getPlanet().length() == 0 ||
                ship.getPlanet().length() > 50 ||
                prodCal.get(Calendar.YEAR) > 3019 ||
                prodCal.get(Calendar.YEAR) < 2800 ||
                ship.getSpeed() > 0.99 ||
                ship.getSpeed() < 0.01 ||
                ship.getCrewSize() < 1 ||
                ship.getCrewSize() > 9999
        )
            throw new BadRequestException();
    }

    private void setRating(Ship ship) {
        prodCal.setTime(ship.getProdDate());
        BigDecimal rating = new BigDecimal(80 * ship.getSpeed() *(ship.getUsed() ? 0.5 : 1)/(3019- prodCal.get(Calendar.YEAR) + 1));
        ship.setRating(rating.setScale(2, RoundingMode.HALF_UP).doubleValue());
    }

    private Long idToLong(String id) {
        try {
            Long idChecked = Long.parseLong(id);
            if (idChecked <= 0)
                throw new Exception();
            return idChecked;
        } catch (Exception e) {
            throw new BadRequestException();
        }
    }
}
