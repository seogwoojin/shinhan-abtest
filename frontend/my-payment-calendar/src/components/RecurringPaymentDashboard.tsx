import React, { useState, useEffect } from 'react';
import { Calendar, Clock, CreditCard, TrendingUp, CheckCircle, AlertCircle, ChevronRight, Settings, RefreshCw, X } from 'lucide-react';

// 타입 정의
interface RecurringPayment {
  merchant_name: string;
  frequency: number;
  average_amount: number;
  amount_variance: number;
  total_amount: number;
  confidence_score: number;
  average_interval_days: number;
  category_code: string | null;
  transaction_dates: string[];
  source: string;
}

interface AutoPaymentSetup {
  merchant_name: string;
  predicted_amount: number;
  predicted_date: string;
  account_number: string;
  is_active: boolean;
}

interface AutoPaymentSaveRequest {
  username: string;
  payments: AutoPaymentSetup[];
}


interface RegularPayment {
  id: number;
  merchantName: string;
  predictedAmount: number;
  predictedDate: string;
  accountNumber: string;
  isActive: boolean;
}

interface RegularPaymentResponse {
  id: number;
  username: string;
  payments: RegularPayment[];
}

interface CalendarEventInfo {
  merchant_name: string;
  predicted_amount: number;
  predicted_date: string;
  category: string | null;
  confidence_score: number;
}

interface CalendarEventRequest {
  username: string;
  events: CalendarEventInfo[];
}

interface CalendarEventResponse {
  result: 'success' | 'error';
  message: string;
  created_events: number;
}

interface PredictedPayment {
  merchant_name: string;
  predicted_amount: number;
  predicted_date: string;
  confidence_score: number;
  frequency_per_month: number;
}

interface RecurringData {
  analysis_period: {
    start_date: string;
    end_date: string;
    days: number;
  };
  recurring_payments: RecurringPayment[];
  total_recurring_merchants: number;
  total_recurring_amount: number;
  monthly_average_recurring: number;
  category_breakdown: Record<string, any>;
  high_confidence_count: number;
}

interface PredictionData {
  next_month: string;
  predicted_payments: PredictedPayment[];
  total_predicted_amount: number;
  prediction_confidence: number;
  generated_at: number;
}

interface ApiResponse<T> {
  result: 'success' | 'error';
  data?: T;
  message?: string;
}

interface CategoryInfo {
  name: string;
  color: string;
}

interface CombinedPayment extends RecurringPayment {
  predicted_amount: number;
  predicted_date: string;
  category: string;
  color: string;
}

interface DashboardProps {
  username?: string;
}

const RecurringPaymentDashboard: React.FC<DashboardProps> = ({ username = "석우진" }) => {
  const [selectedPayments, setSelectedPayments] = useState<Set<number>>(new Set());
  const [showCalendarModal, setShowCalendarModal] = useState<boolean>(false);
  const [showTransferModal, setShowTransferModal] = useState<boolean>(false);
  const [showSettingsModal, setShowSettingsModal] = useState<boolean>(false);
  const [selectedAccount, setSelectedAccount] = useState<string>('신한은행 주거래통장 (****2350)');
  const [recurringData, setRecurringData] = useState<RecurringData | null>(null);
  const [predictionData, setPredictionData] = useState<PredictionData | null>(null);
  const [autoPayments, setAutoPayments] = useState<RegularPayment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // API 호출 함수들
  const fetchRecurringAnalysis = async (): Promise<void> => {
    try {
      const response = await fetch(`http://localhost:8080/toss/api/recurring/analysis/${username}`);
      
      // 응답 상태 확인
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      // 응답 텍스트 먼저 확인
      const responseText = await response.text();
      if (!responseText) {
        throw new Error('서버에서 빈 응답을 받았습니다.');
      }

      // JSON 파싱 시도
      let data: ApiResponse<RecurringData>;
      try {
        data = JSON.parse(responseText);
      } catch (parseError) {
        console.error('JSON 파싱 오류:', responseText);
        throw new Error('서버 응답을 해석할 수 없습니다.');
      }

      if (data.result === 'success' && data.data) {
        setRecurringData(data.data);
      } else {
        throw new Error(data.message || '데이터를 불러올 수 없습니다.');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('정기 지출 분석 API 오류:', err);
    }
  };

  const fetchPrediction = async (): Promise<void> => {
    try {
      const response = await fetch(`http://localhost:8080/toss/api/recurring/prediction/${username}`);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const responseText = await response.text();
      if (!responseText) {
        throw new Error('서버에서 빈 응답을 받았습니다.');
      }

      let data: ApiResponse<PredictionData>;
      try {
        data = JSON.parse(responseText);
      } catch (parseError) {
        console.error('JSON 파싱 오류:', responseText);
        throw new Error('서버 응답을 해석할 수 없습니다.');
      }

      if (data.result === 'success' && data.data) {
        setPredictionData(data.data);
      } else {
        throw new Error(data.message || '예측 데이터를 불러올 수 없습니다.');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('정기 지출 예측 API 오류:', err);
    }
  };

  // 자동 결제 설정 저장 API
  const saveAutoPayments = async (payments: AutoPaymentSetup[]): Promise<void> => {
    try {
      const requestData: AutoPaymentSaveRequest = {
        username,
        payments
      };

      const response = await fetch('http://localhost:8080/toss/api/recurring/auto-payment', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData)
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const responseText = await response.text();
      if (!responseText) {
        // 성공적인 POST 요청의 경우 빈 응답일 수 있음
        return;
      }

      let data: ApiResponse<any>;
      try {
        data = JSON.parse(responseText);
      } catch (parseError) {
        // POST 성공의 경우 빈 응답이 정상일 수 있음
        return;
      }

      if (data.result !== 'success') {
        throw new Error(data.message || '자동 결제 설정 저장에 실패했습니다.');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '자동 결제 설정 저장 중 오류가 발생했습니다.';
      throw new Error(errorMessage);
    }
  };

  // 자동 결제 설정 조회 API (중첩된 payments 배열 구조에 맞게 수정)
  const fetchAutoPayments = async (): Promise<void> => {
    try {
      const response = await fetch(`http://localhost:8080/toss/api/recurring/auto-payment/${username}`);
      
      if (!response.ok) {
        if (response.status === 404) {
          // 404는 데이터가 없다는 의미이므로 빈 배열로 설정
          setAutoPayments([]);
          return;
        }
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const responseText = await response.text();
      if (!responseText) {
        setAutoPayments([]);
        return;
      }

      let data: RegularPaymentResponse[];
      try {
        // 백엔드에서 RegularPaymentResponse 배열을 반환
        data = JSON.parse(responseText);
      } catch (parseError) {
        console.error('JSON 파싱 오류:', responseText);
        setAutoPayments([]);
        return;
      }

      // 배열인지 확인하고 첫 번째 항목의 payments 배열 추출
      if (Array.isArray(data) && data.length > 0 && data[0].payments) {
        setAutoPayments(data[0].payments);
      } else if (Array.isArray(data) && data.length === 0) {
        setAutoPayments([]);
      } else {
        console.error('예상과 다른 데이터 형식:', data);
        setAutoPayments([]);
      }
    } catch (err) {
      console.error('자동 결제 설정 조회 API 오류:', err);
      setAutoPayments([]);
    }
  };

  // 자동 결제 설정 업데이트 API
  const updateAutoPayment = async (id: number, isActive: boolean): Promise<void> => {
    try {
      const response = await fetch(`http://localhost:8080/toss/api/recurring/auto-payment/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ isActive: isActive }) // 백엔드 필드명에 맞게 수정
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const responseText = await response.text();
      if (responseText) {
        let data: ApiResponse<any>;
        try {
          data = JSON.parse(responseText);
          if (data.result !== 'success') {
            throw new Error(data.message || '자동 결제 설정 업데이트에 실패했습니다.');
          }
        } catch (parseError) {
          // PUT 성공의 경우 응답이 없을 수 있음
        }
      }
      
      // 로컬 상태 업데이트
      setAutoPayments(prev => 
        prev.map(payment => 
          payment.id === id ? { ...payment, isActive: isActive } : payment
        )
      );
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '자동 결제 설정 업데이트 중 오류가 발생했습니다.';
      alert(errorMessage);
    }
  };

  // 캘린더 등록 API
  const registerCalendarEvents = async (selectedIndexes: Set<number>): Promise<CalendarEventResponse> => {
    try {
      const events: CalendarEventInfo[] = Array.from(selectedIndexes).map(index => {
        const payment = combinedPayments[index];
        return {
          merchant_name: payment.merchant_name,
          predicted_amount: payment.predicted_amount,
          predicted_date: payment.predicted_date,
          category: payment.category_code,
          confidence_score: payment.confidence_score
        };
      });

      const requestData: CalendarEventRequest = {
        username,
        events
      };

      const response = await fetch('http://localhost:8080/toss/api/calendar/recurring-payments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData)
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const responseText = await response.text();
      if (!responseText) {
        throw new Error('서버에서 빈 응답을 받았습니다.');
      }

      let data: CalendarEventResponse;
      try {
        data = JSON.parse(responseText);
      } catch (parseError) {
        console.error('JSON 파싱 오류:', responseText);
        throw new Error('서버 응답을 해석할 수 없습니다.');
      }

      if (data.result !== 'success') {
        throw new Error(data.message || '캘린더 등록에 실패했습니다.');
      }

      return data;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '캘린더 등록 중 오류가 발생했습니다.';
      throw new Error(errorMessage);
    }
  };

  // 캘린더 등록 처리
  const handleCalendarRegistration = async (): Promise<void> => {
    try {
      const result = await registerCalendarEvents(selectedPayments);
      
      setShowCalendarModal(false);
      setSelectedPayments(new Set());
      alert(`캘린더에 등록되었습니다!\n${result.message}`);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '캘린더 등록 중 오류가 발생했습니다.';
      alert(errorMessage);
    }
  };

  // 초기 데이터 로드
  useEffect(() => {
    const loadData = async (): Promise<void> => {
      setLoading(true);
      setError(null);
      try {
        // 순차적으로 실행하여 에러 추적 용이하게 함
        await fetchRecurringAnalysis();
        await fetchPrediction(); 
        await fetchAutoPayments();
      } catch (err) {
        console.error('데이터 로딩 오류:', err);
        setError('데이터 로딩 중 오류가 발생했습니다.');
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [username]);

  // 데이터 새로고침
  const refreshData = (): void => {
    setLoading(true);
    setError(null);
    
    const loadAllData = async () => {
      try {
        await fetchRecurringAnalysis();
        await fetchPrediction();
        await fetchAutoPayments();
      } catch (err) {
        console.error('데이터 새로고침 오류:', err);
        setError('데이터 새로고침 중 오류가 발생했습니다.');
      } finally {
        setLoading(false);
      }
    };
    
    loadAllData();
  };

  // 카테고리 매핑 함수
  const getCategoryInfo = (merchantName: string, categoryCode: string | null): CategoryInfo => {
    if (categoryCode) {
      const categoryMap: Record<string, CategoryInfo> = {
        'UTILITY': { name: '공과금', color: 'bg-yellow-500' },
        'INSURANCE': { name: '보험료', color: 'bg-green-500' },
        'TELECOM': { name: '통신비', color: 'bg-purple-500' },
        'RENT': { name: '주거비', color: 'bg-blue-500' },
        'FOOD': { name: '식비', color: 'bg-red-500' },
        'TRANSPORT': { name: '교통비', color: 'bg-indigo-500' }
      };
      return categoryMap[categoryCode] || { name: '기타', color: 'bg-gray-500' };
    }

    // 가맹점 이름으로 카테고리 추론
    const name = merchantName.toLowerCase();
    if (name.includes('전력') || name.includes('가스') || name.includes('수도')) {
      return { name: '공과금', color: 'bg-yellow-500' };
    } else if (name.includes('보험')) {
      return { name: '보험료', color: 'bg-green-500' };
    } else if (name.includes('통신') || name.includes('skt') || name.includes('kt') || name.includes('lg')) {
      return { name: '통신비', color: 'bg-purple-500' };
    } else if (name.includes('월세') || name.includes('임대')) {
      return { name: '주거비', color: 'bg-blue-500' };
    }
    return { name: '기타', color: 'bg-gray-500' };
  };

  const formatNumber = (num: number): string => {
    return new Intl.NumberFormat('ko-KR').format(num);
  };

  const formatConfidencePercent = (score: number): string => {
    return `${Math.round(score * 100)}%`;
  };

  const togglePaymentSelection = (index: number): void => {
    const newSelected = new Set(selectedPayments);
    if (newSelected.has(index)) {
      newSelected.delete(index);
    } else {
      newSelected.add(index);
    }
    setSelectedPayments(newSelected);
  };

  // 예약 이체 등록 처리
  const handleAutoPaymentRegistration = async (): Promise<void> => {
    try {
      const selectedPaymentData: AutoPaymentSetup[] = Array.from(selectedPayments).map(index => {
        const payment = combinedPayments[index];
        return {
          merchant_name: payment.merchant_name,
          predicted_amount: payment.predicted_amount,
          predicted_date: payment.predicted_date,
          account_number: selectedAccount,
          is_active: true
        };
      });

      await saveAutoPayments(selectedPaymentData);
      await fetchAutoPayments(); // 설정 목록 새로고침
      
      setShowTransferModal(false);
      setSelectedPayments(new Set());
      alert(`${selectedPaymentData.length}개의 자동 결제가 등록되었습니다!`);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '자동 결제 등록 중 오류가 발생했습니다.';
      alert(errorMessage);
    }
  };

  // 로딩 상태
  if (loading) {
    return (
      <div className="max-w-4xl mx-auto p-4 sm:p-6 bg-gray-50 min-h-screen">
        <div className="bg-white rounded-2xl p-8 sm:p-12 text-center shadow-sm">
          <RefreshCw className="w-8 h-8 text-blue-600 mx-auto mb-4 animate-spin" />
          <p className="text-gray-600">정기 지출 데이터를 분석하고 있습니다...</p>
        </div>
      </div>
    );
  }

  // 에러 상태
  if (error) {
    return (
      <div className="max-w-4xl mx-auto p-4 sm:p-6 bg-gray-50 min-h-screen">
        <div className="bg-white rounded-2xl p-8 sm:p-12 text-center shadow-sm">
          <AlertCircle className="w-8 h-8 text-red-500 mx-auto mb-4" />
          <h2 className="text-lg font-semibold text-gray-900 mb-2">데이터를 불러올 수 없습니다</h2>
          <p className="text-gray-600 mb-4">{error}</p>
          <button 
            onClick={refreshData}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            다시 시도
          </button>
        </div>
      </div>
    );
  }

  // 데이터가 없는 경우
  if (!recurringData || !predictionData) {
    return (
      <div className="max-w-4xl mx-auto p-4 sm:p-6 bg-gray-50 min-h-screen">
        <div className="bg-white rounded-2xl p-8 sm:p-12 text-center shadow-sm">
          <CheckCircle className="w-8 h-8 text-gray-400 mx-auto mb-4" />
          <h2 className="text-lg font-semibold text-gray-900 mb-2">정기 지출 데이터가 없습니다</h2>
          <p className="text-gray-600">충분한 거래 데이터가 쌓이면 정기 지출 패턴을 분석해드립니다.</p>
        </div>
      </div>
    );
  }

  const recurringPayments = recurringData.recurring_payments || [];
  const predictedPayments = predictionData.predicted_payments || [];
  
  // 예측 데이터와 기존 데이터 결합
  const combinedPayments: CombinedPayment[] = recurringPayments.map((payment) => {
    const prediction = predictedPayments.find(p => p.merchant_name === payment.merchant_name);
    const categoryInfo = getCategoryInfo(payment.merchant_name, payment.category_code);
    
    return {
      ...payment,
      predicted_amount: prediction?.predicted_amount || payment.average_amount,
      predicted_date: prediction?.predicted_date || '예측 불가',
      category: categoryInfo.name,
      color: categoryInfo.color
    };
  });

  const totalAmount = predictionData.total_predicted_amount || 0;

  return (
    <div className="max-w-4xl mx-auto p-4 sm:p-6 bg-gray-50 min-h-screen">
      {/* 헤더 */}
      <div className="bg-white rounded-2xl p-4 sm:p-6 mb-4 sm:mb-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between space-y-4 sm:space-y-0">
          <div className="flex-1">
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 mb-2">
              {predictionData.next_month} 정기 지출 예상
            </h1>
            <p className="text-gray-600 text-sm sm:text-base">AI가 분석한 다음 달 예상 지출 내역입니다</p>
          </div>
          <div className="flex items-center justify-between sm:justify-end space-x-4">
            <button 
              onClick={refreshData}
              className="p-2 text-gray-400 hover:text-gray-600 transition-colors"
              title="새로고침"
            >
              <RefreshCw className="w-5 h-5" />
            </button>
            <div className="text-right">
              <p className="text-xs sm:text-sm text-gray-500">총 예상 금액</p>
              <p className="text-xl sm:text-2xl font-bold text-blue-600">{formatNumber(totalAmount)}원</p>
            </div>
          </div>
        </div>
      </div>

      {/* 요약 카드 - 모바일에서는 2열로 */}
      <div className="grid grid-cols-2 lg:grid-cols-3 gap-3 sm:gap-4 mb-4 sm:mb-6">
        <div className="bg-white rounded-xl p-4 sm:p-5 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center sm:space-x-3">
            <div className="w-8 h-8 sm:w-10 sm:h-10 bg-blue-100 rounded-lg flex items-center justify-center mb-2 sm:mb-0">
              <TrendingUp className="w-4 h-4 sm:w-5 sm:h-5 text-blue-600" />
            </div>
            <div>
              <p className="text-xs sm:text-sm text-gray-500">정기 지출 항목</p>
              <p className="text-lg sm:text-xl font-bold text-gray-900">{recurringPayments.length}개</p>
            </div>
          </div>
        </div>
        
        <div className="bg-white rounded-xl p-4 sm:p-5 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center sm:space-x-3">
            <div className="w-8 h-8 sm:w-10 sm:h-10 bg-green-100 rounded-lg flex items-center justify-center mb-2 sm:mb-0">
              <CheckCircle className="w-4 h-4 sm:w-5 sm:h-5 text-green-600" />
            </div>
            <div>
              <p className="text-xs sm:text-sm text-gray-500">높은 신뢰도</p>
              <p className="text-lg sm:text-xl font-bold text-gray-900">{recurringData.high_confidence_count}개</p>
            </div>
          </div>
        </div>
        
        <div className="bg-white rounded-xl p-4 sm:p-5 shadow-sm col-span-2 lg:col-span-1">
          <div className="flex flex-col sm:flex-row sm:items-center sm:space-x-3">
            <div className="w-8 h-8 sm:w-10 sm:h-10 bg-yellow-100 rounded-lg flex items-center justify-center mb-2 sm:mb-0">
              <Clock className="w-4 h-4 sm:w-5 sm:h-5 text-yellow-600" />
            </div>
            <div>
              <p className="text-xs sm:text-sm text-gray-500">분석 기간</p>
              <p className="text-lg sm:text-xl font-bold text-gray-900">
                {recurringData.analysis_period.days}일
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 정기 지출 목록 */}
      <div className="bg-white rounded-2xl shadow-sm overflow-hidden mb-4 sm:mb-6">
        <div className="p-4 sm:p-6 border-b border-gray-100">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">정기 지출 목록</h2>
            <div className="flex items-center space-x-2">
              <span className="text-sm text-gray-500">
                {selectedPayments.size}개 선택됨
              </span>
            </div>
          </div>
        </div>
        
        <div className="divide-y divide-gray-100">
          {combinedPayments.map((payment, index) => {
            const isSelected = selectedPayments.has(index);
            
            return (
              <div 
                key={index}
                className={`p-4 sm:p-6 hover:bg-gray-50 transition-colors cursor-pointer ${
                  isSelected ? 'bg-blue-50 border-l-4 border-blue-500' : ''
                }`}
                onClick={() => togglePaymentSelection(index)}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-3 flex-1">
                    <div className={`w-3 h-3 rounded-full ${payment.color} mt-1`}></div>
                    <div className="flex-1 min-w-0">
                      <h3 className="font-medium text-gray-900 truncate">{payment.merchant_name}</h3>
                      <div className="flex flex-col sm:flex-row sm:items-center sm:space-x-4 mt-1 space-y-1 sm:space-y-0">
                        <span className="text-sm text-gray-500">{payment.category}</span>
                        <span className="text-sm font-medium text-blue-600">
                          신뢰도 {formatConfidencePercent(payment.confidence_score)}
                        </span>
                        <span className="text-sm text-gray-500">
                          예상일: {payment.predicted_date}
                        </span>
                      </div>
                    </div>
                  </div>
                  
                  <div className="text-right ml-3">
                    <p className="text-base sm:text-lg font-semibold text-gray-900">
                      {formatNumber(payment.predicted_amount)}원
                    </p>
                    <div className="flex items-center justify-end space-x-1 mt-1">
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={() => togglePaymentSelection(index)}
                        className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
                        onClick={(e) => e.stopPropagation()}
                      />
                      <span className="text-sm text-gray-500">선택</span>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 액션 버튼 */}
      <div className="space-y-3 sm:space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
          <button
            onClick={() => setShowCalendarModal(true)}
            disabled={selectedPayments.size === 0}
            className="flex items-center justify-center space-x-3 p-4 bg-blue-600 text-white rounded-xl font-medium hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
          >
            <Calendar className="w-5 h-5" />
            <span>캘린더에 등록하기</span>
            <ChevronRight className="w-4 h-4" />
          </button>
          
          <button
            onClick={() => setShowTransferModal(true)}
            disabled={selectedPayments.size === 0}
            className="flex items-center justify-center space-x-3 p-4 bg-green-600 text-white rounded-xl font-medium hover:bg-green-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
          >
            <CreditCard className="w-5 h-5" />
            <span>예약 이체 등록</span>
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
        
        <button 
          className="w-full flex items-center justify-center space-x-3 p-4 bg-gray-100 text-gray-700 rounded-xl font-medium hover:bg-gray-200 transition-colors"
          onClick={() => setShowSettingsModal(true)}
        >
          <Settings className="w-5 h-5" />
          <span>정기 지출 설정 관리</span>
        </button>
      </div>

      {/* 캘린더 모달 */}
      {showCalendarModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-semibold text-gray-900">캘린더 등록</h3>
              <button 
                onClick={() => setShowCalendarModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <div className="space-y-4">
              <div className="p-4 bg-blue-50 rounded-xl">
                <div className="flex items-center space-x-3">
                  <Calendar className="w-5 h-5 text-blue-600" />
                  <span className="font-medium text-blue-900">선택된 {selectedPayments.size}개 항목</span>
                </div>
                <p className="text-sm text-blue-700 mt-2">
                  다음달과 다다음달 캘린더에 일정이 등록됩니다.<br/>
                  결제 예정일 3일 전 알림도 함께 설정됩니다.
                </p>
              </div>
              
              <div className="max-h-40 overflow-y-auto space-y-2">
                {Array.from(selectedPayments).map(index => {
                  const payment = combinedPayments[index];
                  return (
                    <div key={index} className="flex justify-between items-center p-2 bg-gray-50 rounded-lg text-sm">
                      <span className="truncate">{payment.merchant_name}</span>
                      <span className="font-medium ml-2 flex-shrink-0">{payment.predicted_date}</span>
                    </div>
                  );
                })}
              </div>
              
              <div className="flex space-x-3">
                <button 
                  onClick={() => setShowCalendarModal(false)}
                  className="flex-1 py-3 px-4 bg-gray-100 text-gray-700 rounded-xl font-medium hover:bg-gray-200 transition-colors"
                >
                  취소
                </button>
                <button 
                  onClick={handleCalendarRegistration}
                  className="flex-1 py-3 px-4 bg-blue-600 text-white rounded-xl font-medium hover:bg-blue-700 transition-colors"
                >
                  등록하기
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 예약 이체 모달 */}
      {showTransferModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-semibold text-gray-900">예약 이체 등록</h3>
              <button 
                onClick={() => setShowTransferModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <div className="space-y-4">
              <div className="p-4 bg-green-50 rounded-xl">
                <div className="flex items-center space-x-3">
                  <CreditCard className="w-5 h-5 text-green-600" />
                  <span className="font-medium text-green-900">자동 결제 설정</span>
                </div>
                <p className="text-sm text-green-700 mt-2">
                  선택된 {selectedPayments.size}개 항목을 자동 결제로 등록합니다.
                </p>
              </div>
              
              <div className="max-h-40 overflow-y-auto space-y-2">
                {Array.from(selectedPayments).map(index => {
                  const payment = combinedPayments[index];
                  return (
                    <div key={index} className="flex justify-between items-center p-2 bg-gray-50 rounded-lg text-sm">
                      <span className="truncate">{payment.merchant_name}</span>
                      <span className="font-medium ml-2 flex-shrink-0">{formatNumber(payment.predicted_amount)}원</span>
                    </div>
                  );
                })}
              </div>
              
              <div className="p-4 border rounded-xl">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  출금 계좌 선택
                </label>
                <select 
                  className="w-full p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                  value={selectedAccount}
                  onChange={(e) => setSelectedAccount(e.target.value)}
                >
                  <option value="신한은행 주거래통장 (****2350)">신한은행 주거래통장 (****2350)</option>
                  <option value="신한은행 정기예금 (****0000)">신한은행 정기예금 (****0000)</option>
                  <option value="국민은행 자유입출금 (****1080)">국민은행 자유입출금 (****1080)</option>
                </select>
              </div>
              
              <div className="flex space-x-3">
                <button 
                  onClick={() => setShowTransferModal(false)}
                  className="flex-1 py-3 px-4 bg-gray-100 text-gray-700 rounded-xl font-medium hover:bg-gray-200 transition-colors"
                >
                  취소
                </button>
                <button 
                  onClick={handleAutoPaymentRegistration}
                  className="flex-1 py-3 px-4 bg-green-600 text-white rounded-xl font-medium hover:bg-green-700 transition-colors"
                >
                  등록하기
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 정기 지출 설정 관리 모달 */}
      {showSettingsModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-2xl max-h-[80vh] overflow-hidden flex flex-col">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-semibold text-gray-900">자동 결제 설정 관리</h3>
              <button 
                onClick={() => setShowSettingsModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <div className="flex-1 overflow-y-auto">
              {autoPayments.length === 0 ? (
                <div className="text-center py-12">
                  <Settings className="w-12 h-12 text-gray-300 mx-auto mb-4" />
                  <h4 className="text-lg font-medium text-gray-900 mb-2">등록된 자동 결제가 없습니다</h4>
                  <p className="text-gray-500">정기 지출 항목을 선택하여 자동 결제를 등록해보세요.</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {autoPayments.map((payment) => (
                    <div key={payment.id} className="p-4 border rounded-xl">
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <h4 className="font-medium text-gray-900">{payment.merchantName}</h4>
                          <div className="text-sm text-gray-500 mt-1 space-y-1">
                            <p>예상 금액: {formatNumber(payment.predictedAmount)}원</p>
                            <p>예상 결제일: {payment.predictedDate}</p>
                            <p>출금 계좌: {payment.accountNumber}</p>
                          </div>
                        </div>
                        <div className="ml-4 flex flex-col items-end space-y-2">
                          <div className="flex items-center space-x-2">
                            <span className={`text-xs px-2 py-1 rounded-full ${
                              payment.isActive 
                                ? 'bg-green-100 text-green-700' 
                                : 'bg-gray-100 text-gray-700'
                            }`}>
                              {payment.isActive ? '활성' : '비활성'}
                            </span>
                          </div>
                          <label className="flex items-center space-x-2 cursor-pointer">
                            <input
                              type="checkbox"
                              checked={payment.isActive}
                              onChange={(e) => updateAutoPayment(payment.id, e.target.checked)}
                              className="w-4 h-4 text-green-600 rounded focus:ring-green-500"
                            />
                            <span className="text-sm text-gray-600">활성화</span>
                          </label>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
            
            <div className="mt-6 pt-4 border-t">
              <div className="flex justify-between items-center text-sm text-gray-600">
                <span>총 {autoPayments.length}개의 자동 결제 설정</span>
                <span>활성: {autoPayments.filter(p => p.isActive).length}개</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default RecurringPaymentDashboard;