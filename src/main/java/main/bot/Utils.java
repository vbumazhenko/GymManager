package main.bot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Utils {

    public static Date stringToDate(String dateStr) {

        Date date = new Date();
        try {
            date = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;

    }

    public static String dateToString(Date date) {

        return new SimpleDateFormat("yyyy-MM-dd").format(date);

    }

    public static Date stringToTime(String timeStr) {

        Date time = null;
        try {
            time = new SimpleDateFormat("HH:mm:ss").parse(timeStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time;

    }

    public static String timeToString(Date time) {

        return new SimpleDateFormat("H:mm").format(time);

    }

    public static int dayOfWeek(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return 7 - (8 - calendar.get(Calendar.DAY_OF_WEEK))%7; // Делаем первым днем понедельник, а не воскресенье

    }

    public static boolean isValidName(String name) {
        return name.matches("[А-Я][^А-Я]+\\s[А-Я][^А-Я]+");
    }

    public static String getValidPhoneNumber(String phone) {

        String numOnly = phone.replaceAll("[^0-9]", "");

        if (numOnly.length() < 10) {
            return null;
        }

        // Получим номер телефона без 8 и +7 - это последние 10 знаков.
        numOnly = numOnly.substring(numOnly.length() - 10);

        // Преобразуем номер в формат +7 999 999-99-99.
        return "+7 " +
                numOnly.substring(0, 3) +
                " " +
                numOnly.substring(3, 6) +
                "-" +
                numOnly.substring(6, 8) +
                "-" +
                numOnly.substring(8, 10);

    }

    public static boolean notAccessSubscribe(BotContext context) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(context.getSchedule().getTime());

        long endTimeWorkoutMillis = context.getDate().getTime()
                + 3600000 * calendar.get(Calendar.HOUR_OF_DAY) + 60000 * calendar.get(Calendar.MINUTE)
                + (long) (3600000 * context.getSchedule().getWorkoutType().getDuration());
        return endTimeWorkoutMillis < System.currentTimeMillis();

    }

}
