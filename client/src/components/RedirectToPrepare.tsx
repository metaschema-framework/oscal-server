import { useEffect } from 'react';
import { useHistory } from 'react-router-dom';

const RedirectToPrepare: React.FC = () => {
  const history = useHistory();
  
  useEffect(() => {
    history.replace('/prepare');
  }, [history]);
  
  return null;
};

export default RedirectToPrepare;
