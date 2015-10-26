package ru.rzd.dayresult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.concurrent.Callable;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import ru.rzd.otchet.data.AgentState;

/**
 *
 * @author ATonevitskiy
 */
public class DayResultTask implements Callable<Void> {

    private Operator operator;
    private DAODayResult dao;
    private Row row;
    private Calendar date;

    public DayResultTask(Operator operator, DAODayResult dao, Row row, Calendar date) {
        super();
        this.dao = dao;
        this.operator = operator;
        this.row = row;
        this.date = date;
    }

    @Override
    public Void call() throws Exception {
        int id = dao.getID(operator.getSurname(), operator.getInitials());
        operator.setId(id);

        ResultSet rs = dao.getAgentStates(id, date);
        while (rs.next()) {
            AgentState state = AgentState.getByCode(rs.getInt(1));
            Timestamp time = rs.getTimestamp(2);
            operator.addState(state, time);
        }
        if (operator.getLoginTime() != null) {
            rs = dao.getCallDetail(id, operator.getLoginTime());
            while (rs.next()) {
                int ring = rs.getInt(1);
                int talk = rs.getInt(2);
                int hold = rs.getInt(3);
                int work = rs.getInt(4);
                operator.addTimes(ring, talk, hold, work);
            }
            addRows();
        }
        return null;
    }

    private void addRows() {
//        System.out.println("addrow " + operator.getSurname() + " " + operator.getTalkTime());
        if (operator.getId() > 0 && row != null && operator.getAllCalls() > 0 && operator.getStaffTime() > 0) {
            Cell cell;
            BigDecimal staffTime = new BigDecimal(operator.getStaffTime());
            BigDecimal allCalls = new BigDecimal(operator.getAllCalls());

            // stafftime
            cell = row.getCell(1);
            cell.setCellValue(staffTime.divide(new BigDecimal(3600), 2, RoundingMode.HALF_EVEN).doubleValue());
            // время диалога %
            cell = row.getCell(2);
            cell.setCellValue(new BigDecimal(operator.getTalkTime()).divide(staffTime, 3, RoundingMode.HALF_EVEN).doubleValue());
            // время ожидания звонка.
            cell = row.getCell(3);
            cell.setCellValue(new BigDecimal(operator.getWaitTime()).divide(staffTime, 3, RoundingMode.HALF_EVEN).doubleValue());
            // переведенные звонки
            // cell = row.getCell(4);
//            cell.setCellValue(operator.getWaitTime() / operator.getStaffTime()*100);

            // % UTZ
            cell = row.getCell(5);

            cell.setCellValue(new BigDecimal(operator.getStaffTime() - operator.getUnpaidTime())
                    .divide(staffTime, 3, RoundingMode.HALF_EVEN).doubleValue());

            // Поствызовная обработка, %
            cell = row.getCell(6);
            cell.setCellValue(operator.getWorkTime() / operator.getStaffTime());
            // Ring Time (среднее время на 1 звонок), сек
            cell = row.getCell(7);
            cell.setCellValue(operator.getRingTime() / operator.getAllCalls());
            // Состояние "недоступен для приема входящих звонков" (Обед+Перерыв).
            cell = row.getCell(8);
            cell.setCellValue(operator.getUnpaidTime() / operator.getAllCalls());
            // Состояние "недоступен для приема входящих звонков" (Обед+Перерыв).
            cell = row.getCell(9);
            cell.setCellValue(operator.getUnpaidTime());
            // Всего звонков, распределенных на диспетчера, шт
            cell = row.getCell(10);
            cell.setCellValue(operator.getAllCalls());
            // Принятых звонков
            cell = row.getCell(11);
            cell.setCellValue(operator.getAllCalls() - operator.getMissCalls());
            // Средняя продолжительность диалога (секунд)
            cell = row.getCell(12);
            cell.setCellValue(operator.getTalkTime() / (operator.getAllCalls() - operator.getMissCalls()));
            //максимальное время разговора.
            cell = row.getCell(13);
            cell.setCellValue(operator.getMaxTalkTime());
            // пропущенных вызовов
            cell = row.getCell(14);
            cell.setCellValue(operator.getMissCalls());
// пропущенные вызовы
            cell = row.getCell(15);
            cell.setCellValue(new BigDecimal(operator.getMissCalls()).divide(allCalls).doubleValue());
        }
    }

}
