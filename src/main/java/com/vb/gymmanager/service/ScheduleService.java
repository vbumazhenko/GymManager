package com.vb.gymmanager.service;

import com.vb.gymmanager.bot.ChatBot;
import com.vb.gymmanager.bot.Utils;
import com.vb.gymmanager.bot.ScheduleDay;
import com.vb.gymmanager.model.Schedule;
import com.vb.gymmanager.repository.ScheduleRepo;
import com.vb.gymmanager.repository.WorkoutTypeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ChatBot bot;

    @Autowired
    private WorkoutTypeRepo workoutTypeRepo;

    @Autowired
    private ScheduleRepo scheduleRepo;

    public List<ScheduleDay> getScheduleDayList() {

        List<ScheduleDay> scheduleDayList = new ArrayList<>();
        DataSource ds = appContext.getBean(DataSource.class);

        String queryStr =
                "SELECT * FROM (\n" +
                "SELECT s.id, s.weekday, s.time, w.id AS workoutId, w.code AS workoutCode, w.name AS workoutName, SUM(COALESCE(ss.count, 0)) AS count, CAST(MAX(CAST(COALESCE(ss.user_id = %1$d, false) AS INT)) AS BOOLEAN) AS subscribed, CAST(MAX(CAST(COALESCE(ss.user_id = %1$d AND ss.not_sure, false) AS INT)) AS BOOLEAN) AS notSure\n" +
                "FROM schedule s\n" +
                "JOIN (SELECT Max(date) AS date FROM schedule WHERE gym_id = %3$d AND date <= '%2$s' AND weekday = %4$d) AS max_date\n" +
                "   ON s.date >= max_date.date\n" +
                "LEFT JOIN subscribe ss\n" +
                "   ON (ss.date = '%2$s' AND s.time = ss.time AND s.gym_id = ss.gym_id)\n" +
                "LEFT JOIN workout_types w\n" +
                "   ON s.workout_type_id = w.id\n" +
                "WHERE s.gym_id = %3$d AND s.weekday = %4$d\n" +
                "GROUP BY s.id, s.weekday, s.time, w.id\n" +
                "UNION\n" +
                "SELECT s.id, s.weekday, s.time, w.id AS workoutId, w.code AS workoutCode, w.name AS workoutName, SUM(COALESCE(ss.count, 0)) AS count, CAST(MAX(CAST(COALESCE(ss.user_id = %1$d, false) AS INT)) AS BOOLEAN) AS subscribed, CAST(MAX(CAST(COALESCE(ss.user_id = %1$d AND ss.not_sure, false) AS INT)) AS BOOLEAN) AS notSure\n" +
                "FROM schedule s\n" +
                "LEFT JOIN subscribe ss\n" +
                "   ON (ss.date = '%2$s' AND s.time = ss.time AND s.gym_id = ss.gym_id)\n" +
                "LEFT JOIN workout_types w\n" +
                "   ON s.workout_type_id = w.id\n" +
                "WHERE s.gym_id = %3$d AND s.weekday = 0 AND s.date = '%2$s'\n" +
                "GROUP BY s.id, s.weekday, s.time, w.id) AS t\n" +
                "ORDER BY t.time";

        queryStr = String.format(queryStr,
                bot.getUser().getId(),
                Utils.dateToString(bot.getDate()),
                bot.getGym().getId(),
                Utils.dayOfWeek(bot.getDate())
        );

        try {
            Connection connection = ds.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(queryStr);

            while (resultSet.next()) {
                ScheduleDay scheduleDay = new ScheduleDay();
                scheduleDay.setId(resultSet.getInt("id"));
                scheduleDay.setWeekday(resultSet.getInt("weekday"));
                scheduleDay.setTime(new java.util.Date(resultSet.getTime("time").getTime()));
                scheduleDay.setWorkoutId(resultSet.getInt("workoutId"));
                scheduleDay.setWorkoutCode(resultSet.getString("workoutCode"));
                scheduleDay.setWorkoutName(resultSet.getString("workoutName"));
                scheduleDay.setCount(resultSet.getInt("count"));
                scheduleDay.setSubscribed(resultSet.getBoolean("subscribed"));
                scheduleDay.setNotSure(resultSet.getBoolean("notSure"));
                scheduleDayList.add(scheduleDay);
            }
            statement.close();
            connection.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<ScheduleDay> filteredList = scheduleDayList.stream()
                .filter(s -> s.getWeekday() == 0)
                .collect(Collectors.toList());
        if (filteredList.size() > 0) {
            return filteredList;
        } else {
            return scheduleDayList;
        }
    }

    public void copyScheduleOnDay() {

        List<ScheduleDay> scheduleDayList = getScheduleDayList();

        List<Schedule> newScheduleList = new ArrayList<>();

        for (ScheduleDay scheduleDay : scheduleDayList) {

            // Если расписание уже скопировано, то ничего не делаем.
            if (scheduleDay.getWeekday() == 0) {
                return;
            }

            // В противном случае произведем копирование расписания на текущий день.
            Schedule newSchedule = new Schedule();
            newSchedule.setDate(bot.getDate());
            newSchedule.setTime(scheduleDay.getTime());
            newSchedule.setWeekday(0);
            workoutTypeRepo.findById(scheduleDay.getWorkoutId()).ifPresent(newSchedule::setWorkoutType);
            newSchedule.setGym(bot.getUser().getDefaultGym());
            newScheduleList.add(newSchedule);

        }
        scheduleRepo.saveAll(newScheduleList);

    }

}
