package start.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import start.entity.Room;
import start.entity.User;

import java.util.List;

@Repository

public interface RoomRepository extends JpaRepository<Room, Integer> {

    List<Room> findRoomsByUsersIsContaining(User user);
    Room findRoomByUsersIsContainingAndUsersIsContaining(User user1, User user2);

    Room findRoomByRoomID(int roomID);
}
