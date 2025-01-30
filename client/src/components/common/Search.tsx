import React, { useState, useEffect } from 'react';
import { IonSearchbar, IonList, IonItem, IonLabel } from '@ionic/react';
import { useLocation } from 'react-router-dom';

interface SearchResult {
  title: string;
  description: string;
  link: string;
  type: string;
}

interface SearchProps {
  context?: string;
}

export const Search: React.FC<SearchProps> = ({ context }) => {
  const [searchText, setSearchText] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const location = useLocation();

  // Function to search content based on context
  const searchContent = (query: string) => {
    if (!query) {
      setResults([]);
      return;
    }

    // If at root, search all content
    if (!context) {
      // TODO: Implement global search across all pages and content types
      return;
    }

    // Context-specific search (within current page)
    const contextSpecificResults: SearchResult[] = [];
    // TODO: Implement context-specific search logic
    
    setResults(contextSpecificResults);
  };

  useEffect(() => {
    const delaySearch = setTimeout(() => {
      searchContent(searchText);
    }, 300);

    return () => clearTimeout(delaySearch);
  }, [searchText, context]);

  return (
    <div className="ion-padding">
      <IonSearchbar
        value={searchText}
        onIonChange={e => setSearchText(e.detail.value!)}
        placeholder={`Search ${context || 'all content'}...`}
        animated={true}
      />
      {results.length > 0 && (
        <IonList>
          {results.map((result, index) => (
            <IonItem key={index} routerLink={result.link}>
              <IonLabel>
                <h2>{result.title}</h2>
                <p>{result.description}</p>
                <p className="ion-text-end">{result.type}</p>
              </IonLabel>
            </IonItem>
          ))}
        </IonList>
      )}
    </div>
  );
};

export default Search;
