import React, { useState, useEffect } from 'react';
import { 
  IonList, 
  IonItem, 
  IonLabel,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardContent,
  IonBadge,
  IonIcon
} from '@ionic/react';
import { chevronForward, documentText, folderOpen } from 'ionicons/icons';
import { Control, ControlGroup } from '../../types';
import { SearchFilterControls, OscalItem } from './SearchFilterControls';

interface UnifiedSearchFilterProps {
  catalog: {
    controls?: Control[];
    groups?: ControlGroup[];
  };
}

export const UnifiedSearchFilter: React.FC<UnifiedSearchFilterProps> = ({ catalog }) => {
  const [filteredItems, setFilteredItems] = useState<OscalItem[]>([]);
  const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set());

  const handleFilteredItemsChange = (items: OscalItem[]) => {
    setFilteredItems(items);
  };

  // Generate a unique ID for each item
  const getItemKey = (item: OscalItem) => {
    return `${item.type}-${item.id || ''}-${item.level}`;
  };

  // Toggle expanded state for an item
  const toggleExpand = (itemKey: string) => {
    const newExpandedItems = new Set(expandedItems);
    if (expandedItems.has(itemKey)) {
      newExpandedItems.delete(itemKey);
    } else {
      newExpandedItems.add(itemKey);
    }
    setExpandedItems(newExpandedItems);
  };

  // Render an individual item
  const renderItem = (item: OscalItem) => {
    const itemKey = getItemKey(item);
    const isExpanded = expandedItems.has(itemKey);
    
    return (
      <IonCard key={itemKey} className={`item-card level-${item.level}`}>
        <IonItem 
          button 
          detail={false} 
          onClick={() => toggleExpand(itemKey)}
          className="item-header"
        >
          <IonIcon 
            icon={item.type === 'control' ? documentText : folderOpen} 
            slot="start" 
            color={item.type === 'control' ? 'primary' : 'tertiary'}
          />
          <IonLabel>
            <h2>{item.title}</h2>
            <p>{item.id}</p>
          </IonLabel>
          <IonBadge color={item.type === 'control' ? 'primary' : 'tertiary'} slot="end">
            {item.type}
          </IonBadge>
          <IonIcon 
            icon={chevronForward} 
            slot="end" 
            className={isExpanded ? 'expanded-icon' : ''}
          />
        </IonItem>
        
        {isExpanded && (
          <IonCardContent>
            {item.type === 'control' && item.originalControl && (
              <div className="control-details">
                {item.originalControl.props && (
                  <div className="props-container">
                    {item.originalControl.props.map((prop, propIndex) => (
                      <IonBadge key={propIndex} className="prop-chip">
                        {prop.name}: {prop.value}
                        {prop.class && <span className="prop-class">[{prop.class}]</span>}
                      </IonBadge>
                    ))}
                  </div>
                )}
                {item.originalControl.parts && (
                  <div className="parts-container">
                    {item.originalControl.parts.map((part, partIndex) => (
                      <div key={partIndex} className="part-item">
                        <h4>{part.name}{part.title ? `: ${part.title}` : ''}</h4>
                        {part.prose && <p>{part.prose}</p>}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
            
            {item.type === 'group' && item.originalGroup && (
              <div className="group-details">
                {item.originalGroup.props && (
                  <div className="props-container">
                    {item.originalGroup.props.map((prop, propIndex) => (
                      <IonBadge key={propIndex} className="prop-chip">
                        {prop.name}: {prop.value}
                        {prop.class && <span className="prop-class">[{prop.class}]</span>}
                      </IonBadge>
                    ))}
                  </div>
                )}
                {item.originalGroup.parts && (
                  <div className="parts-container">
                    {item.originalGroup.parts.map((part, partIndex) => (
                      <div key={partIndex} className="part-item">
                        <h4>{part.name}{part.title ? `: ${part.title}` : ''}</h4>
                        {part.prose && <p>{part.prose}</p>}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </IonCardContent>
        )}
      </IonCard>
    );
  };

  return (
    <div className="unified-search-filter">
      <SearchFilterControls 
        catalog={catalog}
        onFilteredItemsChange={handleFilteredItemsChange}
      />
      
      <div className="filtered-items-container">
        {filteredItems.length > 0 ? (
          <div className="results-list">
            {filteredItems.map(item => renderItem(item))}
          </div>
        ) : (
          <div className="no-results">
            <p>No items match your search criteria.</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default UnifiedSearchFilter;
