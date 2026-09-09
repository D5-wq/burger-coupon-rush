import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../store/auth';
import { ApiError } from '../types';
import Button from '../components/ui/Button';
import { AuthShell } from './LoginPage';

export default function SignUpPage() {
  const { signUp } = useAuth();
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    if (password.length < 4) {
      setError('비밀번호는 4자 이상이어야 합니다.');
      return;
    }
    setLoading(true);
    try {
      await signUp(email, password, name);
      navigate('/');
    } catch (err) {
      setError((err as ApiError).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell title="회원가입" subtitle="30초면 가입 완료!">
      <form onSubmit={onSubmit} className="space-y-3">
        <input
          className="input"
          placeholder="이름"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <input
          className="input"
          type="email"
          placeholder="이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          autoComplete="email"
          required
        />
        <input
          className="input"
          type="password"
          placeholder="비밀번호 (4자 이상)"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="new-password"
          required
        />
        {error && <p className="text-sm text-red-600">{error}</p>}
        <Button type="submit" size="lg" fullWidth loading={loading}>
          가입하고 시작하기
        </Button>
      </form>
      <p className="mt-5 text-center text-sm text-ink-faint">
        이미 계정이 있으세요?{' '}
        <Link to="/login" className="font-semibold text-brand">
          로그인
        </Link>
      </p>
    </AuthShell>
  );
}
