import {
  IonBadge,
  IonButton,
  IonButtons,
  IonCard,
  IonCardContent,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonModal,
  IonTitle,
  IonToolbar
} from '@ionic/react';
import { ArrayFieldTemplateItemType, ErrorSchema, FormContextType, RJSFSchema, StrictRJSFSchema } from '@rjsf/utils';
import {
  arrowDown,
  arrowUp,
  close,
  copy,
  create,
  trash,
  warning
} from 'ionicons/icons';
import { useState } from 'react';

interface FormContextWithValues {
  formData?: {
    name?: string;
    title?: string;
    label?: string;
    [key: string]: any;
  };
}

interface ExtendedArrayFieldItemType<T = any, S extends StrictRJSFSchema = RJSFSchema, F extends FormContextType = any> 
  extends ArrayFieldTemplateItemType<T, S, F> {
  errorSchema?: ErrorSchema<T>;
}

export default function ArrayFieldItemTemplate<
  T = any,
  S extends StrictRJSFSchema = RJSFSchema,
  F extends FormContextType = any
>(props: ExtendedArrayFieldItemType<T, S, F>) {
  const [isOpen, setIsOpen] = useState(false);
  const {
    children,
    className,
    disabled,
    hasToolbar,
    hasMoveDown,
    hasMoveUp,
    hasRemove,
    hasCopy,
    index,
    onCopyIndexClick,
    onDropIndexClick,
    onReorderClick,
    readonly,
    registry,
    uiSchema,
    errorSchema,
    schema,
  } = props;

  const propertyCount = schema.properties ? Object.keys(schema.properties).length : 0;
  const isSimpleItem = propertyCount <= 3;
  return (
    <>
      <IonItem className={className}>
        <IonLabel onClick={() => !isSimpleItem && setIsOpen(true)} style={{ cursor: isSimpleItem ? 'default' : 'pointer' }}>
          <div className="flex items-center gap-2">
            <div className="flex-1">
              {props.registry.formContext?.formData?.name || 
               props.registry.formContext?.formData?.title || 
               props.registry.formContext?.formData?.label || 
               props.schema.title || 
               `Item ${index + 1}`}
            </div>
            {errorSchema && Object.keys(errorSchema).length > 0 && (
              <div className="flex items-center gap-1">
                <IonIcon 
                  icon={warning} 
                  color="danger" 
                  className="text-lg"
                />
                <IonBadge color="danger">
                  {Object.keys(errorSchema).length}
                </IonBadge>
              </div>
            )}
          </div>
        </IonLabel>
        {hasToolbar && (
          <IonButtons slot="end">
            {!isSimpleItem && (hasMoveUp || hasMoveDown) && (
              <>
                <IonButton
                  disabled={disabled || readonly || !hasMoveUp}
                  onClick={onReorderClick(index, index - 1)}
                  size="small"
                >
                  <IonIcon slot="icon-only" icon={arrowUp} />
                </IonButton>
                <IonButton
                  disabled={disabled || readonly || !hasMoveDown}
                  onClick={onReorderClick(index, index + 1)}
                  size="small"
                >
                  <IonIcon slot="icon-only" icon={arrowDown} />
                </IonButton>
              </>
            )}
            {hasCopy && (
              <IonButton
                disabled={disabled || readonly}
                onClick={onCopyIndexClick(index)}
                size="small"
              >
                <IonIcon slot="icon-only" icon={copy} />
              </IonButton>
            )}
            {!isSimpleItem && (
              <IonButton
                onClick={() => setIsOpen(true)}
                size="small"
                color="primary"
              >
                <IonIcon slot="icon-only" icon={create} />
              </IonButton>
            )}
            {hasRemove && (
              <IonButton
                disabled={disabled || readonly}
                onClick={onDropIndexClick(index)}
                size="small"
                color="danger"
              >
                <IonIcon slot="icon-only" icon={trash} />
              </IonButton>
            )}
          </IonButtons>
        )}
      </IonItem>

      {isSimpleItem ? (
        <div style={{ padding: '0 16px' }}>
          {children}
        </div>
      ) : (
        <IonModal isOpen={isOpen} onDidDismiss={() => setIsOpen(false)}>
        <IonHeader>
          <IonToolbar>
            <IonTitle>Edit Item {index + 1}</IonTitle>
            <IonButtons slot="end">
              <IonButton onClick={() => setIsOpen(false)}>
                <IonIcon slot="icon-only" color='danger' icon={close} />
              </IonButton>
            </IonButtons>
          </IonToolbar>
        </IonHeader>
        <IonContent>
          <IonCard>
            <IonCardContent>
              {children}
            </IonCardContent>
          </IonCard>
        </IonContent>
          <IonButton fill='clear' expand='full' onClick={()=>{setIsOpen(false)}}>Done</IonButton>
        </IonModal>
      )}
    </>
  );
}
