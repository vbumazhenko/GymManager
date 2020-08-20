package com.vb.gymmanager.service;

import com.vb.gymmanager.bot.BotContext;
import com.vb.gymmanager.bot.Utils;
import com.vb.gymmanager.bot.ScheduleDay;
import com.vb.gymmanager.model.Schedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    @Autowired
    private ApplicationContext appContext;

    public List<ScheduleDay> getScheduleDayList(BotContext context) {

        List<ScheduleDay> scheduleDayList = new ArrayList<>();
        DataSource ds = appContext.getBean(DataSource.class);

        String queryStr =
                "SELECT * FROM (\n" +
                "SELECT s.id, s.weekday, s.time, w.id AS workoutId, w.code AS workoutCode, w.name AS workoutName, SUM(COALESCE(ss.count, 0)) AS count, MAX(COALESCE(ss.user_id = %1$d, false)) AS subscribed, MAX(COALESCE(ss.user_id = %1$d AND ss.not_sure, false)) AS notSure\n" +
                "FROM schedule s\n" +
                "JOIN (SELECT Max(date) AS date FROM schedule WHERE gym_id = %3$d AND date <= '%2$s' AND weekday = %4$d) AS max_date\n" +
                "   ON s.date >= max_date.date\n" +
                "LEFT JOIN subscription ss\n" +
                "   ON (ss.date = '%2$s' AND s.time = ss.time AND s.gym_id = ss.gym_id)\n" +
                "LEFT JOIN workout_type w\n" +
                "   ON s.workout_type_id = w.id\n" +
                "WHERE s.gym_id = %3$d AND s.weekday = %4$d\n" +
                "GROUP BY s.id, s.weekday, s.time\n" +
                "UNION\n" +
                "SELECT s.id, s.weekday, s.time, w.id AS workoutId, w.code AS workoutCode, w.name AS workoutName, SUM(COALESCE(ss.count, 0)) AS count, MAX(COALESCE(ss.user_id = %1$d, false)) AS subscribed, MAX(COALESCE(ss.user_id = %1$d AND ss.not_sure, false)) AS notSure\n" +
                "FROM schedule s\n" +
                "LEFT JOIN subscription ss\n" +
                "   ON (ss.date = '%2$s' AND s.time = ss.time AND s.gym_id = ss.gym_id)\n" +
                "LEFT JOIN workout_type w\n" +
                "   ON s.workout_type_id = w.id\n" +
                "WHERE s.gym_id = %3$d AND s.weekday = 0 AND s.date = '%2$s'\n" +
                "GROUP BY s.id, s.weekday, s.time) AS t\n" +
                "ORDER BY t.time";

        queryStr = String.format(queryStr,
                context.getCurrentUser().getId(),
                Utils.dateToString(context.getDate()),
                context.getGym().getId(),
                Utils.dayOfWeek(context.getDate())
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

    public void copyScheduleOnDay(BotContext context) {

        List<ScheduleDay> scheduleDayList = getScheduleDayList(context);

        List<Schedule> newScheduleList = new ArrayList<>();

        for (ScheduleDay scheduleDay : scheduleDayList) {

            // Если расписание уже скопировано, то ничего не делаем.
            if (scheduleDay.getWeekday() == 0) {
                return;
            }

            // В противном случае произведем копирование расписания на текущий день.
            Schedule newSchedule = new Schedule();
            newSchedule.setDate(context.getDate());
            newSchedule.setTime(scheduleDay.getTime());
            newSchedule.setWeekday(0);
            context.workoutTypeRepo.findById(scheduleDay.getWorkoutId()).ifPresent(newSchedule::setWorkoutType);
            newSchedule.setGym(context.getCurrentUser().getDefaultGym());
            newScheduleList.add(newSchedule);

        }
        context.scheduleRepo.saveAll(newScheduleList);

    }

}
