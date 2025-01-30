import React from 'react';
import { IonButton, IonChip, IonInput, IonItem, IonList, IonListHeader, IonNote, IonText, IonTitle } from '@ionic/react';
import { ErrorListProps, SubmitButtonProps, WidgetProps } from '@rjsf/utils';

const IonSubmitButton: React.FC<ErrorListProps> = ({
    errorSchema,errors,
}) => {

  return (
<IonList>
  <IonListHeader>
    <IonTitle color='danger'>Errors</IonTitle>
  </IonListHeader>
  {errors.map(x=><IonItem>
    <IonChip color='danger'>
      {x.property}
    </IonChip>
    <IonText color='warning'>
    {x.message}
    {x.stack}
    </IonText>
  </IonItem>)}
</IonList>

  );
};

export default IonSubmitButton;