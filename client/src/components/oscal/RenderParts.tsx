import React from 'react';
import { IonItem, IonLabel, IonList } from '@ionic/react';
import { Part } from '../../types';
import { RenderProps } from './RenderProps';

interface RenderPartsProps {
  parts: Part[];
}

export const RenderParts: React.FC<RenderPartsProps> = ({ parts }) => {
  if (!parts?.length) return null;

  return (
    <IonList>
      {parts.map((part, index) => (
        <div key={part.id || index} className="part-item ion-padding-vertical">
          {part.title && <h4 className="part-title">{part.title}</h4>}
          {part.prose && <div className="part-prose">{part.prose}</div>}
          {part.props && <RenderProps props={part.props} />}
          {part.parts && part.parts.length > 0 && (
            <div className="nested-content ion-padding">
              <RenderParts parts={part.parts} />
            </div>
          )}
        </div>
      ))}
    </IonList>
  );
};
