import React, { useState, useEffect } from 'react';
import { ChevronLeft, ChevronRight, Search, Edit, Archive, Plus, RefreshCw } from 'lucide-react';

interface Transaction {
  tran_dtime: string;
  tran_amt: number;
  merchant_name: string | null;
  tran_type: string;
  balance_amt: number | null;
  category_code: string | null;
  source: string | null;
}

interface CalendarData {
  [key: string]: Transaction[];
}

interface MonthlySummary {
  year: number;
  month: number;
  total_income: number;
  total_expense: number;
  net_amount: number;
  transaction_count: number;
  category_expenses: { [key: string]: number };
  daily_count: number;
}

const ExpenseCalendar: React.FC = () => {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedDate, setSelectedDate] = useState<Date | null>(new Date());
  const [calendarData, setCalendarData] = useState<CalendarData>({});
  const [monthlySummary, setMonthlySummary] = useState<MonthlySummary | null>(null);
  const [selectedDateTransactions, setSelectedDateTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // 사용자명 - 실제 앱에서는 로그인 정보에서 가져와야 함
  const username = '석우진';

  // 월별 데이터 로드
  const loadMonthlyData = async (year: number, month: number) => {
    setLoading(true);
    setError(null);
    
    try {
      // URL 인코딩된 사용자명 디코딩
      const decodedUsername = decodeURIComponent(username);
      
      // 월별 거래 내역 조회
      const transactionResponse = await fetch(
        `http://localhost:8080/toss/api/monthly/transactions/${encodeURIComponent(decodedUsername)}?year=${year}&month=${month}`,
        {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
          },
          credentials: 'include', // CORS credentials 포함
        }
      );
      
      if (!transactionResponse.ok) {
        throw new Error(`HTTP ${transactionResponse.status}: 거래 내역을 불러오는데 실패했습니다.`);
      }
      
      const transactionData = await transactionResponse.json();
      
      if (transactionData.result === 'success') {
        setCalendarData(transactionData.data || {});
      } else {
        throw new Error(transactionData.message || '거래 내역 조회 실패');
      }
      
      // 월별 요약 정보 조회
      const summaryResponse = await fetch(
        `http://localhost:8080/toss/api/monthly/summary/${encodeURIComponent(decodedUsername)}?year=${year}&month=${month}`,
        {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
          },
          credentials: 'include',
        }
      );
      
      if (summaryResponse.ok) {
        const summaryData = await summaryResponse.json();
        if (summaryData.result === 'success') {
          setMonthlySummary(summaryData.data);
        }
      }
      
    } catch (err) {
      console.error('데이터 로드 오류:', err);
      setError(err instanceof Error ? err.message : '데이터 로드 중 오류가 발생했습니다.');
      // 에러 시 빈 데이터로 설정
      setCalendarData({});
      setMonthlySummary(null);
    } finally {
      setLoading(false);
    }
  };

  // 특정 날짜 거래 내역 로드
  const loadDailyTransactions = async (date: string) => {
    try {
      const decodedUsername = decodeURIComponent(username);
      const response = await fetch(
        `http://localhost:8080/toss/api/monthly/daily/${encodeURIComponent(decodedUsername)}?date=${date}`,
        {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
          },
          credentials: 'include',
        }
      );
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: 일별 거래 내역을 불러오는데 실패했습니다.`);
      }
      
      const data = await response.json();
      
      if (data.result === 'success') {
        setSelectedDateTransactions(data.data || []);
      } else {
        throw new Error(data.message || '일별 거래 내역 조회 실패');
      }
    } catch (err) {
      console.error('일별 거래 내역 로드 실패:', err);
      setSelectedDateTransactions([]);
    }
  };

  // 컴포넌트 마운트 시 및 월 변경 시 데이터 로드
  useEffect(() => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth() + 1; // JavaScript month는 0부터 시작
    loadMonthlyData(year, month);
  }, [currentDate]);

  // 선택된 날짜 변경 시 일별 거래 내역 로드
  useEffect(() => {
    if (selectedDate) {
      const dateKey = getDateKey(selectedDate);
      loadDailyTransactions(dateKey);
    }
  }, [selectedDate]);

  const handleDateClick = (date: Date) => {
    if (date.getMonth() === currentDate.getMonth()) {
      setSelectedDate(date);
    }
  };

  const formatDateDisplay = (date: Date) => {
    const dayNames = ['일', '월', '화', '수', '목', '금', '토'];
    return `${date.getDate()}일 ${dayNames[date.getDay()]}요일`;
  };

  const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'];
  const dayNames = ['일', '월', '화', '수', '목', '금', '토'];

  const getDaysInMonth = (date: Date) => {
    const year = date.getFullYear();
    const month = date.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const daysInMonth = lastDay.getDate();
    const startingDay = firstDay.getDay();

    const days = [];
    
    // Previous month's trailing days
    for (let i = startingDay - 1; i >= 0; i--) {
      const prevDate = new Date(year, month, -i);
      days.push({ date: prevDate, isCurrentMonth: false });
    }
    
    // Current month's days
    for (let day = 1; day <= daysInMonth; day++) {
      days.push({ date: new Date(year, month, day), isCurrentMonth: true });
    }
    
    // Next month's leading days
    const totalCells = Math.ceil(days.length / 7) * 7;
    for (let day = 1; days.length < totalCells; day++) {
      const nextDate = new Date(year, month + 1, day);
      days.push({ date: nextDate, isCurrentMonth: false });
    }
    
    return days;
  };

  const formatNumber = (num: number) => {
    return new Intl.NumberFormat('ko-KR').format(Math.abs(num));
  };

  const getDateKey = (date: Date) => {
    return date.toISOString().split('T')[0];
  };

  const getTotalForDate = (date: Date) => {
    const dateKey = getDateKey(date);
    const transactions = calendarData[dateKey] || [];
    return transactions.reduce((sum, t) => sum + t.tran_amt, 0);
  };

  const goToPreviousMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1));
  };

  const goToNextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1));
  };

  const refreshData = () => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth() + 1;
    loadMonthlyData(year, month);
  };

  const days = getDaysInMonth(currentDate);

  return (
    <div className="max-w-md mx-auto bg-gray-50 min-h-screen">
      {/* Header */}
      <div className="bg-white px-4 py-3 flex items-center justify-between border-b">
        <ChevronLeft 
          className="w-6 h-6 text-gray-600 cursor-pointer hover:text-gray-800" 
          onClick={goToPreviousMonth} 
        />
        <h1 className="text-lg font-medium text-gray-900">
          {monthNames[currentDate.getMonth()]} {currentDate.getFullYear()}
        </h1>
        <div className="flex items-center space-x-3">
          <RefreshCw 
            className={`w-5 h-5 text-gray-600 cursor-pointer hover:text-gray-800 ${loading ? 'animate-spin' : ''}`}
            onClick={refreshData}
          />
          <ChevronRight 
            className="w-6 h-6 text-gray-600 cursor-pointer hover:text-gray-800" 
            onClick={goToNextMonth} 
          />
        </div>
      </div>

      {/* Monthly Summary */}
      {monthlySummary && (
        <div className="bg-white mx-4 mt-4 p-4 rounded-xl shadow-sm">
          <div className="grid grid-cols-3 gap-4 text-center">
            <div>
              <div className="text-xs text-gray-500">수입</div>
              <div className="text-sm font-medium text-blue-600">
                +{formatNumber(monthlySummary.total_income)}
              </div>
            </div>
            <div>
              <div className="text-xs text-gray-500">지출</div>
              <div className="text-sm font-medium text-red-500">
                -{formatNumber(monthlySummary.total_expense)}
              </div>
            </div>
            <div>
              <div className="text-xs text-gray-500">순수익</div>
              <div className={`text-sm font-medium ${monthlySummary.net_amount >= 0 ? 'text-blue-600' : 'text-red-500'}`}>
                {monthlySummary.net_amount >= 0 ? '+' : ''}{formatNumber(monthlySummary.net_amount)}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mx-4 mt-4">
          {error}
        </div>
      )}

      {/* Search Bar */}
      <div className="p-4 bg-white">
        <div className="relative">
          <Search className="w-5 h-5 text-gray-400 absolute left-3 top-1/2 transform -translate-y-1/2" />
          <input
            type="text"
            placeholder="거래 내역 검색"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-3 bg-gray-100 rounded-xl text-gray-900 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      {/* Calendar */}
      <div className="bg-white mx-4 rounded-xl shadow-sm">
        {/* Day Headers */}
        <div className="grid grid-cols-7 border-b">
          {dayNames.map((day) => (
            <div key={day} className="p-3 text-center text-sm text-gray-500 font-medium">
              {day}
            </div>
          ))}
        </div>

        {/* Calendar Days */}
        <div className="grid grid-cols-7">
          {days.map((day, index) => {
            const total = getTotalForDate(day.date);
            const hasTransactions = total !== 0;
            const isToday = day.date.toDateString() === new Date().toDateString();
            const isSelected = selectedDate && day.date.toDateString() === selectedDate.toDateString();
            
            return (
              <div 
                key={index} 
                className={`border-b border-r last:border-r-0 min-h-[80px] p-2 cursor-pointer hover:bg-gray-50 ${
                  isSelected ? 'bg-blue-50' : ''
                } ${!day.isCurrentMonth ? 'opacity-50' : ''}`}
                onClick={() => handleDateClick(day.date)}
              >
                <div className={`text-sm mb-1 ${
                  !day.isCurrentMonth ? 'text-gray-300' : 
                  isToday ? 'text-blue-600 font-bold' : 
                  isSelected ? 'text-blue-600 font-bold' : 'text-gray-900'
                }`}>
                  {day.date.getDate()}
                </div>
                {hasTransactions && day.isCurrentMonth && (
                  <div className="space-y-1">
                    {total > 0 ? (
                      <div className="text-xs text-blue-600 font-medium">
                        +{formatNumber(total)}
                      </div>
                    ) : (
                      <div className="text-xs text-red-500">
                        -{formatNumber(total)}
                      </div>
                    )}
                    {calendarData[getDateKey(day.date)]?.length > 1 && (
                      <div className="text-xs text-gray-400">
                        외 {calendarData[getDateKey(day.date)].length - 1}건
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Selected Date Transactions */}
      <div className="p-4">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-medium text-gray-900">
            {selectedDate ? formatDateDisplay(selectedDate) : '날짜를 선택하세요'}
          </h2>
          <Plus className="w-5 h-5 text-blue-600 cursor-pointer hover:text-blue-800" />
        </div>
        
        {selectedDate && (
          <div className="space-y-3">
            {selectedDateTransactions.length > 0 ? (
              selectedDateTransactions
                .filter(transaction => 
                  !searchTerm || 
                  transaction.merchant_name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                  transaction.tran_type.toLowerCase().includes(searchTerm.toLowerCase())
                )
                .map((transaction, index) => (
                  <div key={index} className="flex items-center space-x-3 p-3 bg-white rounded-lg shadow-sm">
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
                      transaction.tran_amt < 0 ? 'bg-red-100' : 'bg-blue-100'
                    }`}>
                      {transaction.tran_amt < 0 ? (
                        <div className="w-6 h-6 bg-red-500 rounded-full flex items-center justify-center">
                          <span className="text-white text-xs font-bold">-</span>
                        </div>
                      ) : (
                        <div className="w-6 h-6 bg-blue-500 rounded-full flex items-center justify-center">
                          <span className="text-white text-xs font-bold">+</span>
                        </div>
                      )}
                    </div>
                    <div className="flex-1">
                      <div className="text-sm text-gray-900">{transaction.merchant_name || '거래처 정보 없음'}</div>
                      <div className="text-xs text-gray-500">
                        {transaction.tran_type} • {transaction.source}
                        {transaction.category_code && ` • ${transaction.category_code}`}
                      </div>
                      <div className="text-xs text-gray-400">
                        {new Date(transaction.tran_dtime).toLocaleTimeString('ko-KR', { 
                          hour: '2-digit', 
                          minute: '2-digit' 
                        })}
                      </div>
                    </div>
                    <div className={`text-sm font-medium ${
                      transaction.tran_amt < 0 ? 'text-red-500' : 'text-blue-600'
                    }`}>
                      {transaction.tran_amt < 0 ? '-' : '+'}
                      {formatNumber(transaction.tran_amt)} 원
                    </div>
                  </div>
                ))
            ) : (
              <div className="text-center py-8 text-gray-500">
                <div className="mb-2">💰</div>
                <div className="text-sm">
                  {loading ? '거래 내역을 불러오는 중...' : '이 날짜에는 거래 내역이 없습니다'}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default ExpenseCalendar;