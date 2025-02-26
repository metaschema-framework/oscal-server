import React, { useState } from 'react';
import { IonAccordion, IonItem, IonLabel, IonList } from '@ionic/react';
import { Control, Property } from '../../types';
import { RenderProps } from './RenderProps';
import { RenderParts } from './RenderParts';
import { SearchFilterControls, OscalItem } from './SearchFilterControls';

interface RenderControlsProps {
  controls: Control[];
  isNested?: boolean;
}

export const RenderControls: React.FC<RenderControlsProps> = ({ controls, isNested = false }) => {
  const [filteredItems, setFilteredItems] = useState<OscalItem[]>([]);
  
  if (!controls?.length) return null;
  
  const handleFilteredItemsChange = (items: OscalItem[]) => {
    setFilteredItems(items);
  };
  
  // Get only control items that match the original controls
  const getFilteredControls = () => {
    if (filteredItems.length === 0) {
      return controls;
    }
    
    return filteredItems
      .filter(item => item.type === 'control')
      .map(item => item.originalControl)
      .filter((control): control is Control => control !== undefined);
  };
  
  const content = (
    <>
      {!isNested && (
        <SearchFilterControls 
          catalog={{ controls }} 
          onFilteredItemsChange={handleFilteredItemsChange} 
        />
      )}
      <div className="controls-container">
        <IonList>
          {getFilteredControls().map((control, index) => (
            <IonItem key={control.id || index}>
              <IonLabel>
                <h2>{control.title}</h2>
                <p>ID: {control.id}</p>
                {control.props && <RenderProps props={control.props} />}
                {control.parts && <RenderParts parts={control.parts} />}
              </IonLabel>
            </IonItem>
          ))}
        </IonList>
      </div>
    </>
  );

  if (isNested) {
    return (
      <div className="nested-content ion-padding">
        <div className="nested-header">
          <h3>Controls</h3>
        </div>
        <IonList>
          {controls.map((control, index) => (
            <IonItem key={control.id || index}>
              <IonLabel>
                <h2>{control.title}</h2>
                <p>ID: {control.id}</p>
                {control.props && <RenderProps props={control.props} />}
                {control.parts && <RenderParts parts={control.parts} />}
              </IonLabel>
            </IonItem>
          ))}
        </IonList>
      </div>
    );
  }

  return (
    <IonAccordion value="controls">
      <IonItem slot="header" color="light">
        <IonLabel>Controls</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        {content}
      </div>
    </IonAccordion>
  );
};
