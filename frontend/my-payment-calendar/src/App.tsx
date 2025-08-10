import { useState } from 'react'
import './App.css'
import './index.css';
import ExpenseCalendar from './components/ExpenseCalendar';
import RecurringPaymentDashboard from './components/RecurringPaymentDashboard';
import { User, LogOut } from 'lucide-react';

function App() {
  const [currentView, setCurrentView] = useState<'calendar' | 'dashboard'>('calendar');
  const [username, setUsername] = useState<string>('');
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(false);
  const [inputValue, setInputValue] = useState<string>('');

  // 로그인 처리
  const handleLogin = () => {
    if (inputValue.trim()) {
      setUsername(inputValue.trim());
      setIsLoggedIn(true);
    }
  };

  // 로그아웃 처리
  const handleLogout = () => {
    setUsername('');
    setIsLoggedIn(false);
    setInputValue('');
    setCurrentView('calendar');
  };

  // Enter 키 처리
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleLogin();
    }
  };

  // 로그인 화면
  if (!isLoggedIn) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="bg-white rounded-2xl p-8 shadow-lg max-w-md w-full mx-4">
          <div className="text-center mb-8">
            <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <User className="w-8 h-8 text-blue-600" />
            </div>
            <h1 className="text-2xl font-bold text-gray-900 mb-2">FinSync</h1>
            <p className="text-gray-600">신한은행 개인 맞춤형 금융 관리 서비스</p>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                사용자 이름
              </label>
              <input
                type="text"
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="이름을 입력하세요"
                className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                autoFocus
              />
            </div>

            <button
              onClick={handleLogin}
              disabled={!inputValue.trim()}
              className="w-full py-3 px-4 bg-blue-600 text-white rounded-xl font-medium hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
            >
              시작하기
            </button>
          </div>

          <div className="mt-6 text-center">
            <p className="text-xs text-gray-500">
              FinSync는 개인정보를 안전하게 보호합니다
            </p>
          </div>
        </div>
      </div>
    );
  }

  // 메인 애플리케이션
  return (
    <div className="min-h-screen bg-gray-50">
      {/* 상단 헤더 */}
      <div className="bg-white border-b border-gray-200">
        <div className="max-w-4xl mx-auto px-4 py-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
                <User className="w-4 h-4 text-blue-600" />
              </div>
              <div>
                <h2 className="font-semibold text-gray-900">{username}님</h2>
                <p className="text-xs text-gray-500">개인 금융 대시보드</p>
              </div>
            </div>
            
            <button
              onClick={handleLogout}
              className="flex items-center space-x-2 px-3 py-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <LogOut className="w-4 h-4" />
              <span className="text-sm">로그아웃</span>
            </button>
          </div>
        </div>
      </div>

      {/* 네비게이션 탭 */}
      <div className="bg-white shadow-sm">
        <div className="max-w-4xl mx-auto">
          <div className="flex">
            <button
              onClick={() => setCurrentView('calendar')}
              className={`flex-1 py-4 px-6 text-center font-medium transition-colors ${
                currentView === 'calendar'
                  ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              📅 지출 캘린더
            </button>
            <button
              onClick={() => setCurrentView('dashboard')}
              className={`flex-1 py-4 px-6 text-center font-medium transition-colors ${
                currentView === 'dashboard'
                  ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              📊 정기 지출 관리
            </button>
          </div>
        </div>
      </div>

      {/* 컨텐츠 영역 */}
      {currentView === 'dashboard' ? (
        <RecurringPaymentDashboard username={username} />
      ) : (
        <ExpenseCalendar username={username} />
      )}
    </div>
  );
}

export default App;